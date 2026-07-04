package com.deb.project.airBnbApp.controller;


import com.deb.project.airBnbApp.dto.HotelDto;
import com.deb.project.airBnbApp.dto.HotelInfoDto;
import com.deb.project.airBnbApp.dto.HotelPriceDto;
import com.deb.project.airBnbApp.dto.HotelSearchRequest;
import com.deb.project.airBnbApp.service.HotelService;
import com.deb.project.airBnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelBrowseController {

    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @GetMapping("/search")
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest){
        var page= inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId){
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }
}
