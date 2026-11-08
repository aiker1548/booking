package com.hotelbooking.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

public class BookingRequest {
    private UUID userId;
    private UUID hotelId;
    private UUID roomId;
    private LocalDate startDate;
    private LocalDate endDate;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getHotelId() { return hotelId; }
    public void setHotelId(UUID hotelId) { this.hotelId = hotelId; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}