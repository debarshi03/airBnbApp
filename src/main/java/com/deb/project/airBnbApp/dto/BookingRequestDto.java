package com.deb.project.airBnbApp.dto;


import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequestDto {
    private Long hotelId;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer roomsCount;
}
