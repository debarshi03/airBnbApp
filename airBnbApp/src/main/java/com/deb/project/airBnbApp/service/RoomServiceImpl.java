package com.deb.project.airBnbApp.service;

import com.deb.project.airBnbApp.dto.RoomDto;
import com.deb.project.airBnbApp.entity.Hotel;
import com.deb.project.airBnbApp.entity.Room;
import com.deb.project.airBnbApp.entity.User;
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
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService{
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;


    @Override
    @Transactional
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Creating a new room in hotel with Id: {} ", hotelId);
        Hotel hotel=hotelRepository.findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID: "+ hotelId));
        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user doesn't own the hotel with id: "+hotelId);
        }

        Room room= modelMapper.map(roomDto,Room.class);
        room.setHotel(hotel);
        room=roomRepository.save(room);

        if (hotel.getActive()){
            inventoryService.initializeRoomForYear(room);
        }
        return modelMapper.map(room,RoomDto.class);

    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all rooms in hotel with Id: {} ", hotelId);
        Hotel hotel=hotelRepository.findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found with ID: "+ hotelId));

        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user doesn't own the hotel with id: "+hotelId);
        }
        return hotel.getRooms()
                .stream()
                .map((element) -> modelMapper.map(element, RoomDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDto getRoomById(Long id) {
        log.info("Getting room with Id: {} ", id);
        Room room=roomRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID: "+ id));


        return modelMapper.map(room,RoomDto.class);
    }

    @Override
    @Transactional
    public void deleteRoomById(Long id) {
        Room room=roomRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Room not found with ID: "+ id));
        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!user.equals(room.getHotel().getOwner())){
            throw new UnAuthorisedException("This user doesn't own this room with id: "+id);
        }

        inventoryService.deleteFutureInventories(room);
        roomRepository.deleteById(id);

    }
}
