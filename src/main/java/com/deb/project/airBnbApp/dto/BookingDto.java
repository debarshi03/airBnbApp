package com.deb.project.airBnbApp.dto;

import com.deb.project.airBnbApp.entity.Guest;
import com.deb.project.airBnbApp.entity.Hotel;
import com.deb.project.airBnbApp.entity.Room;
import com.deb.project.airBnbApp.entity.User;
import com.deb.project.airBnbApp.entity.enums.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class BookingDto {
    private Long id;
    private HotelDto hotel;
    private RoomDto room;
    private Integer roomsCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookingStatus bookingStatus;
    private Set<GuestDto> guests;
    private BigDecimal amount;
}
