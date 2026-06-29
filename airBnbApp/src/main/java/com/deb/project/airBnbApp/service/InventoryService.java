package com.deb.project.airBnbApp.service;

import com.deb.project.airBnbApp.dto.HotelDto;
import com.deb.project.airBnbApp.dto.HotelPriceDto;
import com.deb.project.airBnbApp.dto.HotelSearchRequest;
import com.deb.project.airBnbApp.entity.Room;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface InventoryService {

    void initializeRoomForYear(Room room);

    void deleteFutureInventories(Room room);

    Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest);
}
