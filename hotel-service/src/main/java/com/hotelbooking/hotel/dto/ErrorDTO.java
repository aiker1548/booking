package com.hotelbooking.hotel.dto;

import java.time.LocalDateTime;

public class ErrorDTO {
    private LocalDateTime timestamp;
    private int status;
    private String code;
    private String message;
    private String path;

    public ErrorDTO(int status, String code, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
