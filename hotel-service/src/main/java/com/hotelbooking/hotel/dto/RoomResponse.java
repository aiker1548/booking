package com.hotelbooking.hotel.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class RoomResponse {
    private UUID id;
    private UUID hotelId;
    private String number;
    private boolean available;
    private int timesBooked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RoomResponse() {}

    public RoomResponse(UUID id, UUID hotelId, String number, boolean available, int timesBooked,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.hotelId = hotelId;
        this.number = number;
        this.available = available;
        this.timesBooked = timesBooked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getHotelId() { return hotelId; }
    public void setHotelId(UUID hotelId) { this.hotelId = hotelId; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public int getTimesBooked() { return timesBooked; }
    public void setTimesBooked(int timesBooked) { this.timesBooked = timesBooked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
