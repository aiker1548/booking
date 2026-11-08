-- Booking Service Initial Schema
-- Spring Boot 3.5.0 + PostgreSQL 16+

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP
);

-- Bookings table
CREATE TABLE bookings (
    booking_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    hotel_id UUID NOT NULL,
    room_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    request_id UUID NOT NULL,
    created_at TIMESTAMP
);

-- Saga states table
CREATE TABLE saga_states (
    id UUID PRIMARY KEY,
    request_id UUID UNIQUE NOT NULL,
    booking_id UUID,
    room_id UUID,
    status VARCHAR(50) NOT NULL,
    hotel_response TEXT,
    error_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0
);

-- Indexes for performance
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_room_dates ON bookings(room_id, start_date, end_date);
CREATE INDEX idx_saga_request_id ON saga_states(request_id);
CREATE INDEX idx_saga_status ON saga_states(status);
