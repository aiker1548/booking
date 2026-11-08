package com.hotelbooking.booking.dto;

import java.time.LocalDateTime;

public class ErrorDTO {
    private final int status;
    private final String message;
    private final String timestamp;

    public ErrorDTO(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}