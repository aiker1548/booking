package com.hotelbooking.booking.saga;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_states")
public class SagaState {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "request_id", unique = true, nullable = false)
    private UUID requestId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "hotel_response")
    private String hotelResponse;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "retry_count")
    private int retryCount;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_COMPENSATED = "COMPENSATED";
    public static final String STATUS_COMPENSATION_FAILED = "COMPENSATION_FAILED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public SagaState() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SagaState(UUID requestId, UUID bookingId, UUID roomId, String status) {
        this();
        this.requestId = requestId;
        this.bookingId = bookingId;
        this.roomId = roomId;
        this.status = status;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHotelResponse() { return hotelResponse; }
    public void setHotelResponse(String hotelResponse) { this.hotelResponse = hotelResponse; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public void incrementRetryCount() { this.retryCount++; }
}