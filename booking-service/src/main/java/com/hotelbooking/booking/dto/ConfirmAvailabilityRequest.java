package com.hotelbooking.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

public class ConfirmAvailabilityRequest {
    private UUID bookingId;
    private LocalDate checkIn;
    private LocalDate checkOut;

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
}
