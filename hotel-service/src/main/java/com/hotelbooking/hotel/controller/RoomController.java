package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.ErrorDTO;
import com.hotelbooking.hotel.dto.RoomRequest;
import com.hotelbooking.hotel.dto.RoomResponse;
import com.hotelbooking.hotel.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/hotels/{hotelId}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(
            @PathVariable("hotelId") UUID hotelId,
            @RequestBody RoomRequest request) {
        log.info("Creating room for hotel {}", hotelId);
        return roomService.createRoom(hotelId, request);
    }

    @GetMapping("/hotels/{hotelId}/rooms")
    public List<RoomResponse> getRoomsByHotel(@PathVariable("hotelId") UUID hotelId) {
        return roomService.getRoomsByHotel(hotelId);
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoomById(@PathVariable("roomId") UUID roomId) {
        return roomService.getRoomById(roomId);
    }

    @PutMapping("/{roomId}")
    public RoomResponse updateRoom(
            @PathVariable("roomId") UUID roomId,
            @RequestBody RoomRequest request) {
        log.info("Updating room: {}", roomId);
        return roomService.updateRoom(roomId, request);
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteRoom(@PathVariable("roomId") UUID roomId) {
        log.info("Deleting room: {}", roomId);
        roomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/availability")
    public boolean isRoomAvailable(
            @PathVariable("roomId") UUID roomId,
            @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return roomService.isRoomAvailable(roomId, checkIn, checkOut);
    }

    @GetMapping("/hotels/{hotelId}/recommend")
    public List<RoomResponse> recommendRooms(
            @PathVariable("hotelId") UUID hotelId,
            @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return roomService.recommendRooms(hotelId, checkIn, checkOut);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleNotFound(IllegalArgumentException ex) {
        return new ErrorDTO(404, "NOT_FOUND", ex.getMessage(), "/rooms");
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDTO handleConflict(IllegalStateException ex) {
        return new ErrorDTO(409, "ROOM_NOT_AVAILABLE", ex.getMessage(), "/rooms");
    }
}
