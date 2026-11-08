package com.hotelbooking.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    private UUID bookingId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "hotel_id")
    private UUID hotelId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Booking() {}

    public Booking(UUID bookingId, UUID userId, UUID hotelId, UUID roomId, LocalDate startDate, LocalDate endDate, BookingStatus status, UUID requestId) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.hotelId = hotelId;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.requestId = requestId;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

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

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}