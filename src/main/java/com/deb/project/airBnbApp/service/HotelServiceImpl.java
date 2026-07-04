package com.deb.project.airBnbApp.service;

import com.deb.project.airBnbApp.dto.HotelDto;
import com.deb.project.airBnbApp.dto.HotelInfoDto;
import com.deb.project.airBnbApp.dto.RoomDto;
import com.deb.project.airBnbApp.entity.Hotel;
import com.deb.project.airBnbApp.entity.Room;
import com.deb.project.airBnbApp.entity.User;
import com.deb.project.airBnbApp.entity.enums.Role;
import com.deb.project.airBnbApp.exception.ResourceNotFoundException;
import com.deb.project.airBnbApp.exception.UnAuthorisedException;
import com.deb.project.airBnbApp.repository.HotelRepository;
import com.deb.project.airBnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {


    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating a new hotel with name: {}",hotelDto.getName());
        Hotel hotel=modelMapper.map(hotelDto,Hotel.class);
        hotel.setActive(false);

        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        hotel.setOwner(user);

        hotel=hotelRepository.save(hotel);
        log.info("Creating a new hotel with id: {}",hotelDto.getId());
        return modelMapper.map(hotel,HotelDto.class);
    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Getting the hotel with ID: {}",id);
        Hotel hotel=hotelRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID: "+ id));
        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user doesn't own the hotel with id: "+id);
        }
        return modelMapper.map(hotel,HotelDto.class);
    }

    @Override
    public List<HotelDto> getAllHotels() {
        User user=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        if (!user.getRoles().contains(Role.HOTEL_MANAGER)){
//            throw new UnAuthorisedException("You not ow any hotels");
//        }
        List<Hotel> hotels=hotelRepository.findByOwner(user);
        return hotels.stream()
                .map((element) -> modelMapper.map(element, HotelDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        log.info("Update the hotel with ID: {}",id);
        Hotel hotel=hotelRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID: "+ id));
        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user doesn't own the hotel with id: "+id);
        }
        modelMapper.map(hotelDto,hotel);
        hotel.setId(id);
        hotel = hotelRepository.save(hotel);
        return modelMapper.map(hotel,HotelDto.class);

    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        Hotel hotel=hotelRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID: "+ id));
        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user doesn't own the hotel with id: "+id);
        }
        for (Room room: hotel.getRooms()){
            inventoryService.deleteFutureInventories(room);
            roomRepository.deleteById(room.getId());
        }
        hotelRepository.deleteById(id);

    }

    @Override
    @Transactional
    public void activateHotel(Long id) {
        log.info("Activating the hotel with ID: {}",id);
        Hotel hotel=hotelRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID: "+ id));
        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user doesn't own the hotel with id: "+id);
        }
        hotel.setActive(true);


        for (Room room: hotel.getRooms()){
            inventoryService.initializeRoomForYear(room);
        }

    }

    @Override
    public HotelInfoDto getHotelInfoById(Long hotelId) {
        Hotel hotel=hotelRepository.findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID: "+ hotelId));

        List<RoomDto> room= hotel.getRooms().stream().map((element) -> modelMapper.map(element, RoomDto.class)).collect(Collectors.toList());
        return new HotelInfoDto(modelMapper.map(hotel,HotelDto.class),room);
    }
}
