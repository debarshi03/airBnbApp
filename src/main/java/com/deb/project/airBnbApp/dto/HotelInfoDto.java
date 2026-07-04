package com.deb.project.airBnbApp.dto;

import com.deb.project.airBnbApp.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HotelInfoDto {
    private HotelDto hotel;
    List<RoomDto> rooms;
}
