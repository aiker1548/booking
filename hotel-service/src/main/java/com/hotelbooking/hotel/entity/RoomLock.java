package com.hotelbooking.hotel.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class RoomLock {

    @Id
    private UUID id;
    private UUID roomId;
    private UUID bookingId;
    private UUID requestId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private RoomLockStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public RoomLock() {}

    public RoomLock(UUID roomId, UUID requestId, LocalDate checkIn, LocalDate checkOut, RoomLockStatus status, LocalDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.requestId = requestId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public RoomLockStatus getStatus() { return status; }
    public void setStatus(RoomLockStatus status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
