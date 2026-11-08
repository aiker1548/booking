
package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.dto.BookingResponse;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.service.BookingService;
import com.hotelbooking.booking.service.SagaOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final SagaOrchestratorService sagaOrchestratorService;

    @Autowired
    public BookingController(BookingService bookingService,
                             SagaOrchestratorService sagaOrchestratorService) {
        this.bookingService = bookingService;
        this.sagaOrchestratorService = sagaOrchestratorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody BookingRequest request,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        request.setUserId(userId);

        log.info("Creating booking for user: {}, room: {}, dates: {}-{}",
                userId, request.getRoomId(), request.getStartDate(), request.getEndDate());

        Booking booking = sagaOrchestratorService.startBookingSaga(request);

        return ResponseEntity.ok(new BookingResponse(booking.getBookingId(), booking.getStatus()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        log.info("Retrieving booking: {}", id);

        return ResponseEntity.ok(bookingService.getBooking(id, userId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BookingResponse>> getUserBookings(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        log.info("Retrieving bookings for user: {}", userId);

        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        log.info("Cancelling booking: {}", id);

        sagaOrchestratorService.cancelBookingSaga(id, userId);

        return ResponseEntity.ok().build();
    }
}