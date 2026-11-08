-- Hotel Service Initial Schema
-- Spring Boot 3.5.0 + PostgreSQL 16+

-- Hotel table
CREATE TABLE hotel (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(255),
    city VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Room table
CREATE TABLE room (
    id UUID PRIMARY KEY,
    hotel_id UUID NOT NULL REFERENCES hotel(id),
    number VARCHAR(50) NOT NULL,
    available BOOLEAN DEFAULT true,
    times_booked INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Room lock table
CREATE TABLE room_lock (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES room(id),
    booking_id UUID,
    request_id UUID NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status SMALLINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_room_hotel_id ON room(hotel_id);
CREATE INDEX idx_room_lock_room_id ON room_lock(room_id);
CREATE INDEX idx_room_lock_request_id ON room_lock(request_id);
CREATE INDEX idx_room_lock_status_expiry ON room_lock(status, expires_at);

-- Unique constraint: room number must be unique per hotel
CREATE UNIQUE INDEX idx_room_hotel_number ON room(hotel_id, number);
