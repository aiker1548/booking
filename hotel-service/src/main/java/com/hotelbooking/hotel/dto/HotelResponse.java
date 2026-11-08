package com.hotelbooking.hotel.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class HotelResponse {
    private UUID id;
    private String name;
    private String description;
    private String address;
    private String city;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HotelResponse() {}

    public HotelResponse(UUID id, String name, String description, String address, String city,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.city = city;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
