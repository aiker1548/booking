package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.dto.BookingResponse;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public Booking createBooking(BookingRequest request) {
        log.info("Creating booking for user: {}, room: {}, dates: {}-{}",
            request.getUserId(), request.getRoomId(), request.getStartDate(), request.getEndDate());

        LocalDate endDate = request.getStartDate().plusDays(1);
        Booking booking = new Booking(
            UUID.randomUUID(),
            request.getUserId(),
            request.getHotelId(),
            request.getRoomId(),
            request.getStartDate(),
            endDate,
            BookingStatus.PENDING,
            UUID.randomUUID()
        );

        if (bookingRepository.existsByUserIdAndRoomIdAndStartDateAndEndDate(
            booking.getUserId(), booking.getRoomId(), booking.getStartDate(), booking.getEndDate())) {
            log.warn("Duplicate booking detected for user: {}, room: {}, dates: {}",
                booking.getUserId(), booking.getRoomId(), booking.getStartDate());
            throw new IllegalArgumentException("A booking already exists for this room and dates");
        }

        booking.setStatus(BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void confirmBooking(UUID bookingId, UUID roomId, LocalDate startDate, LocalDate endDate) {
        log.info("Confirming booking: {}, room: {}, startDate: {}, endDate: {}", bookingId, roomId, startDate, endDate);

        Optional<Booking> bookingOptional = bookingRepository.findById(bookingId);
        Booking booking = bookingOptional.orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (!booking.getRoomId().equals(roomId) ||
            !booking.getStartDate().equals(startDate) ||
            !booking.getEndDate().equals(endDate)) {
            throw new RuntimeException("Invalid booking confirmation request");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Booking cannot be confirmed as it is not in PENDING state");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Booking {} confirmed successfully", bookingId);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void cancelBooking(UUID bookingId, UUID userId) {
        log.info("Cancelling booking: {}, user: {}", bookingId, userId);

        Optional<Booking> bookingOptional = bookingRepository.findByBookingIdAndUserId(bookingId, userId);
        Booking booking = bookingOptional.orElseThrow(() -> new RuntimeException("Booking not found or access denied"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel a confirmed booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Booking {} cancelled successfully", bookingId);
    }

    @PreAuthorize("hasRole('USER')")
    public BookingResponse getBooking(UUID bookingId, UUID userId) {
        log.info("Retrieving booking: {} for user: {}", bookingId, userId);

        Optional<Booking> bookingOptional = bookingRepository.findByBookingIdAndUserId(bookingId, userId);
        Booking booking = bookingOptional.orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found or access denied")
        );

        return new BookingResponse(booking.getBookingId(), booking.getStatus());
    }

    @PreAuthorize("hasRole('USER')")
    public List<BookingResponse> getUserBookings(UUID userId) {
        log.info("Retrieving bookings for user: {}", userId);

        List<Booking> bookings = bookingRepository.findByUserId(userId);

        return bookings.stream()
            .map(booking -> new BookingResponse(booking.getBookingId(), booking.getStatus()))
            .collect(Collectors.toList());
    }
}