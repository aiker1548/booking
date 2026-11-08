package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.ConfirmAvailabilityRequest;
import com.hotelbooking.hotel.dto.RoomRequest;
import com.hotelbooking.hotel.dto.RoomResponse;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.RoomLock;
import com.hotelbooking.hotel.entity.RoomLockStatus;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomLockRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomLockRepository roomLockRepository;

    @InjectMocks
    private RoomService roomService;

    private UUID hotelId;
    private UUID roomId;
    private Hotel hotel;
    private Room room;

    @BeforeEach
    void setUp() {
        hotelId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        hotel = new Hotel("Grand Hotel", "Luxury", "123 Main St", "New York");
        hotel.setId(hotelId);

        room = new Room(hotelId, "101");
        room.setId(roomId);
    }

    // --- CRUD ---

    @Test
    void createRoom_shouldCreateRoom() {
        RoomRequest request = new RoomRequest();
        request.setNumber("101");

        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(roomRepository.findByHotelIdAndNumber(hotelId, "101"))
                .thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RoomResponse response = roomService.createRoom(hotelId, request);

        assertThat(response.getNumber()).isEqualTo("101");
        assertThat(response.getHotelId()).isEqualTo(hotelId);
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void createRoom_shouldThrowWhenHotelNotFound() {
        RoomRequest request = new RoomRequest();
        request.setNumber("101");

        when(hotelRepository.existsById(hotelId)).thenReturn(false);

        assertThatThrownBy(() -> roomService.createRoom(hotelId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hotel not found");
    }

    @Test
    void createRoom_shouldThrowWhenDuplicateRoomNumber() {
        RoomRequest request = new RoomRequest();
        request.setNumber("101");

        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(roomRepository.findByHotelIdAndNumber(hotelId, "101"))
                .thenReturn(Optional.of(new Room(hotelId, "101")));

        assertThatThrownBy(() -> roomService.createRoom(hotelId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room number already exists");
    }

    @Test
    void getRoomsByHotel_shouldReturnRooms() {
        Room room2 = new Room(hotelId, "102");
        room2.setId(UUID.randomUUID());

        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(roomRepository.findByHotelId(hotelId))
                .thenReturn(List.of(room, room2));

        List<RoomResponse> responses = roomService.getRoomsByHotel(hotelId);

        assertThat(responses).hasSize(2);
    }

    @Test
    void getRoomsByHotel_shouldThrowWhenHotelNotFound() {
        when(hotelRepository.existsById(hotelId)).thenReturn(false);

        assertThatThrownBy(() -> roomService.getRoomsByHotel(hotelId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getRoomById_shouldReturnRoom() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        RoomResponse response = roomService.getRoomById(roomId);

        assertThat(response.getId()).isEqualTo(roomId);
        assertThat(response.getNumber()).isEqualTo("101");
    }

    @Test
    void getRoomById_shouldThrowWhenNotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomById(roomId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteRoom_shouldDelete() {
        when(roomRepository.existsById(roomId)).thenReturn(true);

        roomService.deleteRoom(roomId);

        verify(roomRepository).deleteById(roomId);
    }

    @Test
    void deleteRoom_shouldThrowWhenNotFound() {
        when(roomRepository.existsById(roomId)).thenReturn(false);

        assertThatThrownBy(() -> roomService.deleteRoom(roomId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Availability ---

    @Test
    void isRoomAvailable_shouldReturnTrueWhenNoLocks() {
        when(roomRepository.existsById(roomId)).thenReturn(true);
        when(roomLockRepository.existsOverlappingLocks(
                eq(roomId),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(RoomLockStatus.CONFIRMED),
                eq(RoomLockStatus.HELD),
                any(LocalDateTime.class)
        )).thenReturn(false);

        boolean available = roomService.isRoomAvailable(
                roomId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5)
        );

        assertThat(available).isTrue();
    }

    @Test
    void isRoomAvailable_shouldReturnFalseWhenLocked() {
        when(roomRepository.existsById(roomId)).thenReturn(true);
        when(roomLockRepository.existsOverlappingLocks(
                eq(roomId),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(RoomLockStatus.CONFIRMED),
                eq(RoomLockStatus.HELD),
                any(LocalDateTime.class)
        )).thenReturn(true);

        boolean available = roomService.isRoomAvailable(
                roomId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5)
        );

        assertThat(available).isFalse();
    }

    @Test
    void isRoomAvailable_shouldThrowWhenRoomNotFound() {
        when(roomRepository.existsById(roomId)).thenReturn(false);

        assertThatThrownBy(() -> roomService.isRoomAvailable(
                roomId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isRoomAvailable_shouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> roomService.isRoomAvailable(
                roomId,
                LocalDate.of(2026, 12, 5),
                LocalDate.of(2026, 12, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Check-out must be after check-in");
    }

    @Test
    void isRoomAvailable_shouldRejectEqualDates() {
        assertThatThrownBy(() -> roomService.isRoomAvailable(
                roomId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Check-out must be after check-in");
    }

    // --- Recommendation ---

    @Test
    void recommendRooms_shouldReturnSortedByTimesBooked() {
        Room roomLow = new Room(hotelId, "101");
        roomLow.setId(UUID.randomUUID());
        roomLow.setTimesBooked(2);
        roomLow.setAvailable(true);

        Room roomHigh = new Room(hotelId, "102");
        roomHigh.setId(UUID.randomUUID());
        roomHigh.setTimesBooked(5);
        roomHigh.setAvailable(true);
        when(roomRepository.existsById(roomLow.getId())).thenReturn(true);

        when(roomRepository.existsById(roomHigh.getId())).thenReturn(true);

        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(roomRepository.findByHotelId(hotelId))
                .thenReturn(List.of(roomHigh, roomLow));

        when(roomLockRepository.existsOverlappingLocks(
                any(UUID.class),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(RoomLockStatus.CONFIRMED),
                eq(RoomLockStatus.HELD),
                any(LocalDateTime.class)
        )).thenReturn(false);

        List<RoomResponse> recommendations = roomService.recommendRooms(
                hotelId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5)
        );

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.get(0).getTimesBooked()).isEqualTo(2);
        assertThat(recommendations.get(1).getTimesBooked()).isEqualTo(5);
    }

    @Test
    void recommendRooms_shouldReturnEmptyWhenNoRoomsAvailable() {
        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(roomRepository.findByHotelId(hotelId)).thenReturn(List.of());

        List<RoomResponse> recommendations = roomService.recommendRooms(
                hotelId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5)
        );

        assertThat(recommendations).isEmpty();
    }

    @Test
    void recommendRooms_shouldThrowWhenHotelNotFound() {
        when(hotelRepository.existsById(hotelId)).thenReturn(false);

        assertThatThrownBy(() -> roomService.recommendRooms(
                hotelId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getRecommendedRoom_shouldReturnFirst() {
        Room recommendedRoom = new Room(hotelId, "101");
        recommendedRoom.setId(roomId);
        recommendedRoom.setTimesBooked(1);
        recommendedRoom.setAvailable(true);

        when(roomRepository.existsById(roomId)).thenReturn(true);

        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        
        when(roomRepository.findByHotelId(hotelId))
                .thenReturn(List.of(recommendedRoom));

        when(roomLockRepository.existsOverlappingLocks(
                any(UUID.class),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(RoomLockStatus.CONFIRMED),
                eq(RoomLockStatus.HELD),
                any(LocalDateTime.class)
        )).thenReturn(false);

        RoomResponse recommended = roomService.getRecommendedRoom(
                hotelId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5)
        );

        assertThat(recommended.getId()).isEqualTo(roomId);
    }

    @Test
    void getRecommendedRoom_shouldThrowWhenNoneAvailable() {
        when(hotelRepository.existsById(hotelId)).thenReturn(true);
        when(roomRepository.findByHotelId(hotelId)).thenReturn(List.of());

        assertThatThrownBy(() -> roomService.getRecommendedRoom(
                hotelId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No rooms available");
    }

    // --- Lock Management ---

    @Test
    void confirmAvailability_shouldCreateLock() {
        UUID requestId = UUID.randomUUID();

        ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest();
        request.setCheckIn(LocalDate.of(2026, 12, 1));
        request.setCheckOut(LocalDate.of(2026, 12, 5));

        when(roomRepository.findByIdWithLock(roomId))
                .thenReturn(Optional.of(room));
        when(roomLockRepository.findByRequestId(requestId))
                .thenReturn(Optional.empty());
        when(roomLockRepository.findOverlappingLocks(
                eq(roomId),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(RoomLockStatus.CONFIRMED),
                eq(RoomLockStatus.HELD),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(roomLockRepository.save(any(RoomLock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoomLock lock = roomService.confirmAvailability(roomId, requestId, request);

        assertThat(lock.getStatus()).isEqualTo(RoomLockStatus.HELD);
        assertThat(lock.getRoomId()).isEqualTo(roomId);
        assertThat(lock.getRequestId()).isEqualTo(requestId);
        assertThat(lock.getCheckIn()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void confirmAvailability_shouldThrowWhenRoomNotFound() {
        UUID requestId = UUID.randomUUID();

        ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest();
        request.setCheckIn(LocalDate.of(2026, 12, 1));
        request.setCheckOut(LocalDate.of(2026, 12, 5));

        when(roomRepository.findByIdWithLock(roomId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                roomService.confirmAvailability(roomId, requestId, request)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmAvailability_shouldThrowWhenRoomNotOperationallyAvailable() {
        UUID requestId = UUID.randomUUID();

        ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest();
        request.setCheckIn(LocalDate.of(2026, 12, 1));
        request.setCheckOut(LocalDate.of(2026, 12, 5));

        Room unavailableRoom = new Room(hotelId, "101");
        unavailableRoom.setId(roomId);
        unavailableRoom.setAvailable(false);

        when(roomRepository.findByIdWithLock(roomId))
                .thenReturn(Optional.of(unavailableRoom));

        assertThatThrownBy(() ->
                roomService.confirmAvailability(roomId, requestId, request)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not operationally available");
    }

    @Test
    void confirmAvailability_shouldThrowWhenOverlappingLockExists() {
        UUID requestId = UUID.randomUUID();

        ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest();
        request.setCheckIn(LocalDate.of(2026, 12, 1));
        request.setCheckOut(LocalDate.of(2026, 12, 5));

        when(roomRepository.findByIdWithLock(roomId))
                .thenReturn(Optional.of(room));
        when(roomLockRepository.findByRequestId(requestId))
                .thenReturn(Optional.empty());

        RoomLock existingLock = new RoomLock(
                roomId,
                UUID.randomUUID(),
                LocalDate.of(2026, 12, 3),
                LocalDate.of(2026, 12, 7),
                RoomLockStatus.HELD,
                LocalDateTime.now().plusMinutes(2)
        );

        when(roomLockRepository.findOverlappingLocks(
                eq(roomId),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(RoomLockStatus.CONFIRMED),
                eq(RoomLockStatus.HELD),
                any(LocalDateTime.class)
        )).thenReturn(List.of(existingLock));

        assertThatThrownBy(() ->
                roomService.confirmAvailability(roomId, requestId, request)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void confirmAvailability_shouldBeIdempotentWithSameRequestId() {
        UUID requestId = UUID.randomUUID();

        ConfirmAvailabilityRequest request = new ConfirmAvailabilityRequest();
        request.setCheckIn(LocalDate.of(2026, 12, 1));
        request.setCheckOut(LocalDate.of(2026, 12, 5));

        when(roomRepository.findByIdWithLock(roomId))
                .thenReturn(Optional.of(room));

        RoomLock existingLock = new RoomLock(
                roomId,
                requestId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                RoomLockStatus.HELD,
                LocalDateTime.now().plusMinutes(2)
        );
        existingLock.setId(UUID.randomUUID());

        when(roomLockRepository.findByRequestId(requestId))
                .thenReturn(Optional.of(existingLock));

        RoomLock lock = roomService.confirmAvailability(roomId, requestId, request);

        assertThat(lock.getId()).isEqualTo(existingLock.getId());
        verify(roomLockRepository, never()).save(any());
        verify(roomLockRepository, never()).findOverlappingLocks(
                any(UUID.class),
                any(LocalDate.class),
                any(LocalDate.class),
                any(RoomLockStatus.class),
                any(RoomLockStatus.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void releaseRoomLock_shouldReleaseExistingLock() {
        UUID requestId = UUID.randomUUID();

        RoomLock lock = new RoomLock(
                roomId,
                requestId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                RoomLockStatus.HELD,
                LocalDateTime.now().plusMinutes(2)
        );
        lock.setId(UUID.randomUUID());

        when(roomLockRepository.findByRequestId(requestId))
                .thenReturn(Optional.of(lock));

        roomService.releaseRoomLock(roomId, requestId);

        assertThat(lock.getStatus()).isEqualTo(RoomLockStatus.RELEASED);
        verify(roomLockRepository).save(lock);
    }

    @Test
    void releaseRoomLock_shouldBeIdempotentForAlreadyReleased() {
        UUID requestId = UUID.randomUUID();

        RoomLock lock = new RoomLock(
                roomId,
                requestId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                RoomLockStatus.RELEASED,
                LocalDateTime.now().plusMinutes(2)
        );
        lock.setId(UUID.randomUUID());

        when(roomLockRepository.findByRequestId(requestId))
                .thenReturn(Optional.of(lock));

        roomService.releaseRoomLock(roomId, requestId);

        assertThat(lock.getStatus()).isEqualTo(RoomLockStatus.RELEASED);
        verify(roomLockRepository, never()).save(any());
    }

    @Test
    void confirmBooking_shouldIncrementTimesBooked() {
        UUID requestId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        RoomLock lock = new RoomLock(
                roomId,
                requestId,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                RoomLockStatus.HELD,
                LocalDateTime.now().plusMinutes(2)
        );
        lock.setId(UUID.randomUUID());

        when(roomLockRepository.findByRequestId(requestId))
                .thenReturn(Optional.of(lock));
        when(roomRepository.findByIdWithLock(roomId))
                .thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        roomService.confirmBooking(roomId, requestId, bookingId);

        assertThat(lock.getStatus()).isEqualTo(RoomLockStatus.CONFIRMED);
        assertThat(lock.getBookingId()).isEqualTo(bookingId);
        assertThat(room.getTimesBooked()).isEqualTo(1);
    }

    @Test
    void confirmBooking_shouldThrowWhenNoLockFound() {
        UUID requestId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(roomLockRepository.findByRequestId(requestId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                roomService.confirmBooking(roomId, requestId, bookingId)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelBooking_shouldDecrementTimesBooked() {
        Room lockedRoom = new Room(hotelId, "101");
        lockedRoom.setId(roomId);
        lockedRoom.setTimesBooked(3);

        UUID bookingId = UUID.randomUUID();

        RoomLock lock = new RoomLock(
                roomId,
                UUID.randomUUID(),
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                RoomLockStatus.CONFIRMED,
                LocalDateTime.now().plusMinutes(2)
        );
        lock.setBookingId(bookingId);

        when(roomLockRepository.findByBookingId(bookingId))
                .thenReturn(Optional.of(lock));
        when(roomRepository.findByIdWithLock(roomId))
                .thenReturn(Optional.of(lockedRoom));

        roomService.cancelBooking(roomId, bookingId);

        assertThat(lockedRoom.getTimesBooked()).isEqualTo(2);
        assertThat(lock.getStatus()).isEqualTo(RoomLockStatus.RELEASED);
    }

    @Test
    void cancelBooking_shouldThrowWhenCounterIsAlreadyZero() {
        Room lockedRoom = new Room(hotelId, "101");
        lockedRoom.setId(roomId);
        lockedRoom.setTimesBooked(0);

        UUID bookingId = UUID.randomUUID();

        RoomLock lock = new RoomLock(
                roomId,
                UUID.randomUUID(),
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                RoomLockStatus.CONFIRMED,
                LocalDateTime.now().plusMinutes(2)
        );
        lock.setBookingId(bookingId);

        when(roomLockRepository.findByBookingId(bookingId))
                .thenReturn(Optional.of(lock));
        when(roomRepository.findByIdWithLock(roomId))
                .thenReturn(Optional.of(lockedRoom));

        assertThatThrownBy(() -> roomService.cancelBooking(roomId, bookingId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timesBooked");

        assertThat(lockedRoom.getTimesBooked()).isZero();
        assertThat(lock.getStatus()).isEqualTo(RoomLockStatus.CONFIRMED);
    }
}
