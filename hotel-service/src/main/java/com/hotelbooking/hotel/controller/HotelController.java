package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.ErrorDTO;
import com.hotelbooking.hotel.dto.HotelRequest;
import com.hotelbooking.hotel.dto.HotelResponse;
import com.hotelbooking.hotel.service.HotelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/hotels")
public class HotelController {

    private static final Logger log = LoggerFactory.getLogger(HotelController.class);

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponse createHotel(@RequestBody HotelRequest request) {
        log.info("Creating hotel: {}", request.getName());
        return hotelService.createHotel(request);
    }

    @GetMapping
    public List<HotelResponse> getAllHotels() {
        return hotelService.getAllHotels();
    }

    @GetMapping("/{hotelId}")
    public HotelResponse getHotelById(@PathVariable("hotelId") UUID hotelId) {
        return hotelService.getHotelById(hotelId);
    }

    @PutMapping("/{hotelId}")
    public HotelResponse updateHotel(
            @PathVariable("hotelId") UUID hotelId,
            @RequestBody HotelRequest request) {
        log.info("Updating hotel: {}", hotelId);
        return hotelService.updateHotel(hotelId, request);
    }

    @DeleteMapping("/{hotelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteHotel(@PathVariable("hotelId") UUID hotelId) {
        log.info("Deleting hotel: {}", hotelId);
        hotelService.deleteHotel(hotelId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleNotFound(IllegalArgumentException ex) {
        return new ErrorDTO(404, "NOT_FOUND", ex.getMessage(), "/hotels");
    }
}
