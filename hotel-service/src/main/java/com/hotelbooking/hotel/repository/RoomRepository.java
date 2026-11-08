package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByHotelId(UUID hotelId);

    Optional<Room> findByHotelIdAndNumber(UUID hotelId, String number);

    @Query("SELECT r FROM Room r WHERE r.id = :id AND r.available = true")
    Optional<Room> findAvailableById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(UUID id);

    @Query("SELECT r FROM Room r WHERE r.hotelId = :hotelId AND r.available = true " +
           "AND r.id NOT IN (" +
           "  SELECT rl.roomId FROM RoomLock rl WHERE rl.status = :status " +
           "  AND rl.checkIn < :checkOut AND rl.checkOut > :checkIn" +
           ") ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableByHotel(UUID hotelId, LocalDate checkIn, LocalDate checkOut, String status);
}
