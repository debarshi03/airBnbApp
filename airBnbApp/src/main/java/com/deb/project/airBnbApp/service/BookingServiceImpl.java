package com.deb.project.airBnbApp.service;

import com.deb.project.airBnbApp.dto.*;
import com.deb.project.airBnbApp.entity.*;
import com.deb.project.airBnbApp.entity.enums.BookingStatus;
import com.deb.project.airBnbApp.exception.ResourceNotFoundException;
import com.deb.project.airBnbApp.exception.UnAuthorisedException;
import com.deb.project.airBnbApp.repository.*;
import com.deb.project.airBnbApp.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.service.PriceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.dialect.function.InverseDistributionWindowEmulation;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.print.Book;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.deb.project.airBnbApp.util.AppUtils.getCurrentUser;


@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService{


    private final GuestRepository guestRepository;


    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    private final InventoryRepository inventoryRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final CheckoutService checkoutService;
    private final PricingService pricingService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequestDto bookingRequestDto) {
        Hotel hotel= hotelRepository.findById(bookingRequestDto.getHotelId()).orElseThrow(()->
                new ResourceNotFoundException("Hotel not found with id: "+bookingRequestDto.getHotelId()));

        Room room=roomRepository.findById(bookingRequestDto.getRoomId()).orElseThrow(()->
                new ResourceNotFoundException("Room not found with id: "+bookingRequestDto.getRoomId()));

        List<Inventory> inventories= inventoryRepository.findAndLockAvailableInventory(bookingRequestDto.getRoomId(),
                bookingRequestDto.getStartDate(),bookingRequestDto.getEndDate(),bookingRequestDto.getRoomsCount());

        long daysCount= ChronoUnit.DAYS.between(bookingRequestDto.getStartDate(),bookingRequestDto.getEndDate())+1;

        if (inventories.size() != daysCount){
            throw new IllegalStateException("Room is not available anymore");
        }

        inventoryRepository.initBooking(bookingRequestDto.getRoomId(), bookingRequestDto.getStartDate(),bookingRequestDto.getEndDate()
                ,bookingRequestDto.getRoomsCount());

        BigDecimal priceForOneRoom = pricingService.calculateTotalPrice(inventories);

        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequestDto.getRoomsCount()));



        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequestDto.getStartDate())
                .checkOutDate(bookingRequestDto.getEndDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequestDto.getRoomsCount())
                .amount(totalPrice)
                .build();
        booking=bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDto.class);
    }

    @Override
    @Transactional
    public BookingDto addGuest(Long bookingId, List<GuestDto> guestDtoList) {
        Booking booking =bookingRepository.findById(bookingId).orElseThrow(()->
                new ResourceNotFoundException("Booking not found with id: "+bookingId));

        User user=getCurrentUser();

        log.info("user with id: "+user.getId());

        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("Booking doesn't belong to this user with id : "+user.getId());
        }

        if (hasBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired");
        }

        if (booking.getBookingStatus()!=BookingStatus.RESERVED){
            throw new IllegalStateException("Booking is not under reserved state, can not add guest");
        }

        for (GuestDto guestDto: guestDtoList){
            Guest guest=modelMapper.map(guestDto, Guest.class);
            guest.setUser(getCurrentUser());
            guest=guestRepository.save(guest);
            booking.getGuests().add(guest);
        }
        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking=bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDto.class);

    }

    @Override
    @Transactional
    public String initiatePayment(Long bookingId) {
        Booking booking=bookingRepository.findById(bookingId)
                .orElseThrow(()-> new ResourceNotFoundException("Booking not found with id: "+bookingId));

        User user=getCurrentUser();

        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("Booking doesn't belong to this user with id : "+user.getId());
        }

        if (hasBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired");
        }

        String sessionUrl=checkoutService.getCheckoutSession(booking,frontendUrl+"/payments/success", frontendUrl+"/payments/failure");
        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);
        bookingRepository.save(booking);
        return sessionUrl;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        if ("checkout.session.completed".equals(event.getType())){
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (event==null) return;

            String sessionId=session.getId();
            Booking booking= bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(() ->
                    new ResourceNotFoundException("Booking not found for session ID: "+sessionId));

            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            log.info("Successfully confirmed the booking for Booking ID: {}", booking.getId());
        } else {
            log.warn("Unhandled event type: {}", event.getType());
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking =bookingRepository.findById(bookingId).orElseThrow(()->
                new ResourceNotFoundException("Booking not found with id: "+bookingId));

        User user=getCurrentUser();

        log.info("user with id: "+user.getId());

        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("Booking doesn't belong to this user with id : "+user.getId());
        }

        if(booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        inventoryRepository.cancelBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        try {
            Session session=Session.retrieve(booking.getPaymentSessionId());

            RefundCreateParams refundCreateParams= RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();

            Refund.create(refundCreateParams);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        return booking.getBookingStatus().name();
    }

    public boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    @Override
    public List<BookingDto> getAllBookingsByHotelId(Long hotelId) {
        Hotel hotel=hotelRepository.findById(hotelId).orElseThrow(()-> new ResourceNotFoundException("Hotel not found with Id: "+hotelId));
        User user=getCurrentUser();

        if(!user.equals(hotel.getOwner())) throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);


        List<Booking> bookings=bookingRepository.findByHotel(hotelId);

        return bookings.stream().map((element) -> modelMapper.map(element, BookingDto.class)).collect(Collectors.toList());
    }
}
