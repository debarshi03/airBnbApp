package com.deb.project.airBnbApp.service;

import com.deb.project.airBnbApp.dto.BookingDto;
import com.deb.project.airBnbApp.dto.BookingRequestDto;
import com.deb.project.airBnbApp.dto.GuestDto;
import com.stripe.model.Event;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface BookingService {

    BookingDto initialiseBooking(BookingRequestDto bookingRequestDto);

    BookingDto addGuest(Long bookingId, List<GuestDto> guestDtoList);

    String initiatePayment(Long bookingId);

    void capturePayment(Event event);

    void cancelBooking(Long bookingId);

    String getBookingStatus(Long bookingId);

    List<BookingDto> getAllBookingsByHotelId(Long hotelId);
}
