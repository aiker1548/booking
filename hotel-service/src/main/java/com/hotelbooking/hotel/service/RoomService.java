package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.*;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.RoomLock;
import com.hotelbooking.hotel.entity.RoomLockStatus;
import com.hotelbooking.hotel.repository.RoomLockRepository;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    private static final int LOCK_EXPIRY_MINUTES = 2;

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomLockRepository roomLockRepository;

    public RoomService(RoomRepository roomRepository, HotelRepository hotelRepository,
                       RoomLockRepository roomLockRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.roomLockRepository = roomLockRepository;
    }

    // --- CRUD ---

    @Transactional
    public RoomResponse createRoom(UUID hotelId, RoomRequest request) {
        log.info("Creating room {} for hotel {}", request.getNumber(), hotelId);

        if (!hotelRepository.existsById(hotelId)) {
            throw new IllegalArgumentException("Hotel not found: " + hotelId);
        }

        Optional<Room> existing = roomRepository.findByHotelIdAndNumber(hotelId, request.getNumber());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Room number already exists: " + request.getNumber());
        }

        Room room = new Room(hotelId, request.getNumber());
        Room saved = roomRepository.save(room);
        log.info("Room created with id: {}", saved.getId());
        return toResponse(saved);
    }

    public List<RoomResponse> getRoomsByHotel(UUID hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new IllegalArgumentException("Hotel not found: " + hotelId);
        }
        return roomRepository.findByHotelId(hotelId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse getRoomById(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        return toResponse(room);
    }

    @Transactional
    public RoomResponse updateRoom(UUID roomId, RoomRequest request) {
        log.info("Updating room: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (request.getNumber() != null) {
            room.setNumber(request.getNumber());
        }
        Room updated = roomRepository.save(room);
        log.info("Room updated: {}", roomId);
        return toResponse(updated);
    }

    @Transactional
    public void deleteRoom(UUID roomId) {
        log.info("Deleting room: {}", roomId);

        if (!roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        roomRepository.deleteById(roomId);
        log.info("Room deleted: {}", roomId);
    }

    public boolean isRoomOperationallyAvailable(UUID roomId) {
        return roomRepository.findById(roomId)
                .map(Room::isAvailable)
                .orElse(false);
    }

    // --- Availability ---

    public boolean isRoomAvailable(UUID roomId, LocalDate checkIn, LocalDate checkOut) {
        validateDateRange(checkIn, checkOut);

        if (!roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        return !roomLockRepository.existsOverlappingLocks(
                roomId,
                checkIn,
                checkOut,
                RoomLockStatus.CONFIRMED,
                RoomLockStatus.HELD,
                LocalDateTime.now()
        );
    }

    public List<RoomResponse> recommendRooms(
            UUID hotelId,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        validateDateRange(checkIn, checkOut);

        if (!hotelRepository.existsById(hotelId)) {
            throw new IllegalArgumentException("Hotel not found: " + hotelId);
        }

        return roomRepository.findByHotelId(hotelId).stream()
                .filter(Room::isAvailable)
                .filter(room -> isRoomAvailable(room.getId(), checkIn, checkOut))
                .sorted(
                        java.util.Comparator.comparingInt(Room::getTimesBooked)
                                .thenComparing(Room::getId)
                )
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse getRecommendedRoom(UUID hotelId, LocalDate checkIn, LocalDate checkOut) {
        List<RoomResponse> rooms = recommendRooms(hotelId, checkIn, checkOut);

        if (rooms.isEmpty()) {
            throw new IllegalStateException("No rooms available for the requested dates");
        }

        return rooms.get(0);
    }

    // --- Internal API (saga integration) ---

    @Transactional
    public RoomLock confirmAvailability(UUID roomId, UUID requestId, ConfirmAvailabilityRequest request) {
        log.info("Confirming availability for room {}, requestId {}", roomId, requestId);

        validateDateRange(request.getCheckIn(), request.getCheckOut());

        Optional<Room> roomOpt = roomRepository.findByIdWithLock(roomId);
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        Room room = roomOpt.get();
        if (!room.isAvailable()) {
            throw new IllegalStateException("Room is not operationally available");
        }

        // Idempotency: return existing lock for the same request.
        Optional<RoomLock> requestLock = roomLockRepository.findByRequestId(requestId);
        if (requestLock.isPresent()) {
            RoomLock lock = requestLock.get();

            if (!lock.getRoomId().equals(roomId)
                    || !lock.getCheckIn().equals(request.getCheckIn())
                    || !lock.getCheckOut().equals(request.getCheckOut())) {
                throw new IllegalStateException("Request ID was already used with different booking data");
            }

            if (lock.getStatus() == RoomLockStatus.HELD
                    && lock.getExpiresAt().isAfter(LocalDateTime.now())) {
                return lock;
            }

            throw new IllegalStateException("Existing lock is no longer active");
        }

        List<RoomLock> overlappingLocks = roomLockRepository.findOverlappingLocks(
                roomId,
                request.getCheckIn(),
                request.getCheckOut(),
                RoomLockStatus.CONFIRMED,
                RoomLockStatus.HELD,
                LocalDateTime.now()
        );

        if (!overlappingLocks.isEmpty()) {
            throw new IllegalStateException("Room not available for the requested dates");
        }

        RoomLock roomLock = new RoomLock(
                roomId,
                requestId,
                request.getCheckIn(),
                request.getCheckOut(),
                RoomLockStatus.HELD,
                LocalDateTime.now().plusMinutes(LOCK_EXPIRY_MINUTES)
        );

        RoomLock saved = roomLockRepository.save(roomLock);
        log.info("Room lock created: {}", saved.getId());

        return saved;
    }

    @Transactional
    public void releaseRoomLock(UUID roomId, UUID requestId) {
        log.info("Releasing room lock for room {}, requestId {}", roomId, requestId);

        roomLockRepository.findByRequestId(requestId).ifPresent(lock -> {
            if (lock.getStatus() == RoomLockStatus.HELD) {
                lock.setStatus(RoomLockStatus.RELEASED);
                roomLockRepository.save(lock);
                log.info("Room lock released: {}", lock.getId());
            }
            // If already RELEASED, idempotent no-op
        });
    }

    @Transactional
    public void confirmBooking(UUID roomId, UUID requestId, UUID bookingId) {
        log.info("Confirming booking {} for room {}, requestId {}", bookingId, roomId, requestId);

        Optional<RoomLock> lockOpt = roomLockRepository.findByRequestId(requestId);
        if (lockOpt.isEmpty()) {
            throw new IllegalArgumentException("No lock found for requestId: " + requestId);
        }

        RoomLock lock = lockOpt.get();
        if (!lock.getRoomId().equals(roomId)) {
            throw new IllegalStateException("Lock does not match room");
        }
        if (lock.getStatus() != RoomLockStatus.HELD) {
            throw new IllegalStateException("Lock is not in HELD status");
        }

        lock.setBookingId(bookingId);
        lock.setStatus(RoomLockStatus.CONFIRMED);
        roomLockRepository.save(lock);

        // Increment timesBooked
        roomRepository.findByIdWithLock(roomId).ifPresent(room -> {
            room.setTimesBooked(room.getTimesBooked() + 1);
            roomRepository.save(room);
        });

        log.info("Booking {} confirmed, timesBooked incremented", bookingId);
    }

    @Transactional
    public void cancelBooking(UUID roomId, UUID bookingId) {
        log.info("Cancelling booking {} for room {}", bookingId, roomId);

        RoomLock lock = roomLockRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking lock not found: " + bookingId
                ));

        if (!lock.getRoomId().equals(roomId)) {
            throw new IllegalStateException("Booking does not belong to this room");
        }

        // Idempotency: decrement only once.
        if (lock.getStatus() != RoomLockStatus.CONFIRMED) {
            log.info("Booking {} is already cancelled or inactive", bookingId);
            return;
        }

        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        if (room.getTimesBooked() <= 0) {
            throw new IllegalStateException("Invalid timesBooked counter for room: " + roomId);
        }

        lock.setStatus(RoomLockStatus.RELEASED);
        room.setTimesBooked(room.getTimesBooked() - 1);

        roomLockRepository.save(lock);
        roomRepository.save(room);

        log.info("Booking {} cancelled, timesBooked decremented", bookingId);
    }

    // --- Helpers ---

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(), room.getHotelId(), room.getNumber(),
                room.isAvailable(), room.getTimesBooked(),
                room.getCreatedAt(), room.getUpdatedAt());
    }
}
