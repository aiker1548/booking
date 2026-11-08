package com.hotelbooking.booking.dto;

import java.util.UUID;
import com.hotelbooking.booking.entity.BookingStatus;

public class BookingResponse {
    private UUID bookingId;
    private BookingStatus status;

    public BookingResponse() {}

    public BookingResponse(UUID bookingId, BookingStatus status) {
        this.bookingId = bookingId;
        this.status = status;
    }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
}