package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.dto.ReleaseLockRequest;
import com.hotelbooking.hotel.entity.RoomLock;
import com.hotelbooking.hotel.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/rooms")
public class InternalRoomController {

    private static final Logger log = LoggerFactory.getLogger(InternalRoomController.class);

    private final RoomService roomService;

    public InternalRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/{roomId}/confirm-availability")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, Object>> confirmAvailability(
            @PathVariable("roomId") UUID roomId,
            @RequestParam("requestId") UUID requestId,
            @RequestBody ConfirmAvailabilityRequest request) {
        log.info("Confirming availability for room {}, requestId {}", roomId, requestId);

        RoomLock lock = roomService.confirmAvailability(roomId, requestId, request);

        Map<String, Object> response = Map.of(
                "lockId", lock.getId().toString(),
                "status", lock.getStatus().name(),
                "checkIn", lock.getCheckIn().toString(),
                "checkOut", lock.getCheckOut().toString(),
                "expiresAt", lock.getExpiresAt().toString()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/release")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, String>> releaseRoomLock(
            @PathVariable("roomId") UUID roomId,
            @RequestBody ReleaseLockRequest request) {
        log.info("Releasing room lock for room {}, requestId {}", roomId, request.getRequestId());

        roomService.releaseRoomLock(roomId, request.getRequestId());
        return ResponseEntity.ok(Map.of("status", "released"));
    }

    @PostMapping("/{roomId}/confirm-booking")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, String>> confirmBooking(
            @PathVariable("roomId") UUID roomId,
            @RequestParam("requestId") UUID requestId,
            @RequestParam("bookingId") UUID bookingId) {
        log.info("Confirming booking {} for room {}, requestId {}", bookingId, roomId, requestId);

        roomService.confirmBooking(roomId, requestId, bookingId);
        return ResponseEntity.ok(Map.of("status", "confirmed"));
    }

    @PostMapping("/{roomId}/cancel-booking")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, String>> cancelBooking(
            @PathVariable("roomId") UUID roomId,
            @RequestParam("bookingId") UUID bookingId) {
        log.info("Cancelling booking {} for room {}", bookingId, roomId);

        roomService.cancelBooking(roomId, bookingId);
        return ResponseEntity.ok(Map.of("status", "cancelled"));
    }
}
