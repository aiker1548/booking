package com.hotelbooking.hotel.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class Room {

    @Id
    private UUID id;
    private UUID hotelId;
    private String number;
    private boolean available;
    private int timesBooked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Room() {}

    public Room(UUID hotelId, String number) {
        this.id = UUID.randomUUID();
        this.hotelId = hotelId;
        this.number = number;
        this.available = true;
        this.timesBooked = 0;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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
