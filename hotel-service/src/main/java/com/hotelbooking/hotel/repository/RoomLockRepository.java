
package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.RoomLock;
import com.hotelbooking.hotel.entity.RoomLockStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomLockRepository extends JpaRepository<RoomLock, UUID> {

    Optional<RoomLock> findByBookingId(UUID bookingId);

    Optional<RoomLock> findByRequestId(UUID requestId);

    List<RoomLock> findByStatusAndExpiresAtBefore(
            RoomLockStatus status,
            LocalDateTime expiresAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT rl FROM RoomLock rl
        WHERE rl.roomId = :roomId
          AND rl.checkIn < :checkOut
          AND rl.checkOut > :checkIn
          AND (
              rl.status = :confirmedStatus
              OR (
                  rl.status = :heldStatus
                  AND rl.expiresAt > :now
              )
          )
        """)
    List<RoomLock> findOverlappingLocks(
            @Param("roomId") UUID roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("confirmedStatus") RoomLockStatus confirmedStatus,
            @Param("heldStatus") RoomLockStatus heldStatus,
            @Param("now") LocalDateTime now
    );
    @Query("""
        SELECT COUNT(rl) > 0
        FROM RoomLock rl
        WHERE rl.roomId = :roomId
        AND rl.checkIn < :checkOut
        AND rl.checkOut > :checkIn
        AND (
            rl.status = :confirmedStatus
            OR (
                rl.status = :heldStatus
                AND rl.expiresAt > :now
            )
        )
        """)
    boolean existsOverlappingLocks(
            @Param("roomId") UUID roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("confirmedStatus") RoomLockStatus confirmedStatus,
            @Param("heldStatus") RoomLockStatus heldStatus,
            @Param("now") LocalDateTime now
    );
}
