package com.hotelbooking.booking.repository;

import com.hotelbooking.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    boolean existsByUserIdAndRoomIdAndStartDateAndEndDate(
            UUID userId,
            UUID roomId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Booking> findByBookingIdAndUserId(UUID bookingId, UUID userId);

    List<Booking> findByUserId(UUID userId);
}