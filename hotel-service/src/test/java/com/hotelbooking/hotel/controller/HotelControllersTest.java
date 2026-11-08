package com.hotelbooking.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.hotel.dto.HotelRequest;
import com.hotelbooking.hotel.dto.HotelResponse;
import com.hotelbooking.hotel.dto.RoomRequest;
import com.hotelbooking.hotel.dto.RoomResponse;
import com.hotelbooking.hotel.dto.ReleaseLockRequest;
import com.hotelbooking.hotel.entity.RoomLock;
import com.hotelbooking.hotel.entity.RoomLockStatus;
import com.hotelbooking.hotel.service.HotelService;
import com.hotelbooking.hotel.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HotelControllersTest {

    @Mock
    private HotelService hotelService;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private HotelController hotelController;

    @InjectMocks
    private RoomController roomController;

    @InjectMocks
    private InternalRoomController internalRoomController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID hotelId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        hotelId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        mockMvc = MockMvcBuilders
                .standaloneSetup(hotelController, roomController, internalRoomController)
                .build();
    }

    // --- Hotel Controller ---

    @Test
    void createHotel_shouldReturn201() throws Exception {
        HotelRequest request = new HotelRequest();
        request.setName("Grand Hotel");
        request.setDescription("Luxury");
        request.setAddress("123 Main St");
        request.setCity("New York");

        HotelResponse response = new HotelResponse(hotelId, "Grand Hotel", "Luxury", "123 Main St", "New York",
                LocalDateTime.now(), LocalDateTime.now());

        when(hotelService.createHotel(any(HotelRequest.class))).thenReturn(response);

        mockMvc.perform(post("/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(hotelId.toString()))
                .andExpect(jsonPath("$.name").value("Grand Hotel"));
    }

    @Test
    void getAllHotels_shouldReturn200() throws Exception {
        HotelResponse hotel = new HotelResponse(hotelId, "Hotel A", "Desc", "Addr", "City",
                LocalDateTime.now(), LocalDateTime.now());

        when(hotelService.getAllHotels()).thenReturn(List.of(hotel));

        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Hotel A"));
    }

    @Test
    void getHotelById_shouldReturn200() throws Exception {
        HotelResponse hotel = new HotelResponse(hotelId, "Hotel A", "Desc", "Addr", "City",
                LocalDateTime.now(), LocalDateTime.now());

        when(hotelService.getHotelById(hotelId)).thenReturn(hotel);

        mockMvc.perform(get("/hotels/{hotelId}", hotelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(hotelId.toString()));
    }

    @Test
    void getHotelById_shouldReturn404WhenNotFound() throws Exception {
        when(hotelService.getHotelById(hotelId))
                .thenThrow(new IllegalArgumentException("Hotel not found: " + hotelId));

        mockMvc.perform(get("/hotels/{hotelId}", hotelId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void updateHotel_shouldReturn200() throws Exception {
        HotelRequest request = new HotelRequest();
        request.setName("Updated");

        HotelResponse response = new HotelResponse(hotelId, "Updated", "Desc", "Addr", "City",
                LocalDateTime.now(), LocalDateTime.now());

        when(hotelService.updateHotel(eq(hotelId), any(HotelRequest.class))).thenReturn(response);

        mockMvc.perform(put("/hotels/{hotelId}", hotelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteHotel_shouldReturn204() throws Exception {
        doNothing().when(hotelService).deleteHotel(hotelId);

        mockMvc.perform(delete("/hotels/{hotelId}", hotelId))
                .andExpect(status().isNoContent());
    }

    // --- Room Controller ---

    @Test
    void createRoom_shouldReturn201() throws Exception {
        RoomRequest request = new RoomRequest();
        request.setNumber("101");

        RoomResponse response = new RoomResponse(roomId, hotelId, "101", true, 0,
                LocalDateTime.now(), LocalDateTime.now());

        when(roomService.createRoom(eq(hotelId), any(RoomRequest.class))).thenReturn(response);

        mockMvc.perform(post("/rooms/hotels/{hotelId}/rooms", hotelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.number").value("101"));
    }

    @Test
    void getRoomsByHotel_shouldReturn200() throws Exception {
        RoomResponse room = new RoomResponse(roomId, hotelId, "101", true, 0,
                LocalDateTime.now(), LocalDateTime.now());

        when(roomService.getRoomsByHotel(hotelId)).thenReturn(List.of(room));

        mockMvc.perform(get("/rooms/hotels/{hotelId}/rooms", hotelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("101"));
    }

    @Test
    void getRoomById_shouldReturn200() throws Exception {
        RoomResponse room = new RoomResponse(roomId, hotelId, "101", true, 0,
                LocalDateTime.now(), LocalDateTime.now());

        when(roomService.getRoomById(roomId)).thenReturn(room);

        mockMvc.perform(get("/rooms/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roomId.toString()));
    }

    @Test
    void getRoomById_shouldReturn404WhenNotFound() throws Exception {
        when(roomService.getRoomById(roomId))
                .thenThrow(new IllegalArgumentException("Room not found: " + roomId));

        mockMvc.perform(get("/rooms/{roomId}", roomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void isRoomAvailable_shouldReturnTrue() throws Exception {
        when(roomService.isRoomAvailable(eq(roomId), eq(LocalDate.of(2026, 12, 1)), eq(LocalDate.of(2026, 12, 5))))
                .thenReturn(true);

        mockMvc.perform(get("/rooms/{roomId}/availability", roomId)
                        .param("checkIn", "2026-12-01")
                        .param("checkOut", "2026-12-05"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void isRoomAvailable_shouldReturnFalse() throws Exception {
        when(roomService.isRoomAvailable(eq(roomId), eq(LocalDate.of(2026, 12, 1)), eq(LocalDate.of(2026, 12, 5))))
                .thenReturn(false);

        mockMvc.perform(get("/rooms/{roomId}/availability", roomId)
                        .param("checkIn", "2026-12-01")
                        .param("checkOut", "2026-12-05"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void recommendRooms_shouldReturnOrdered() throws Exception {
        RoomResponse room1 = new RoomResponse(roomId, hotelId, "101", true, 1,
                LocalDateTime.now(), LocalDateTime.now());
        RoomResponse room2 = new RoomResponse(UUID.randomUUID(), hotelId, "102", true, 3,
                LocalDateTime.now(), LocalDateTime.now());

        when(roomService.recommendRooms(eq(hotelId), eq(LocalDate.of(2026, 12, 1)), eq(LocalDate.of(2026, 12, 5))))
                .thenReturn(List.of(room1, room2));

        mockMvc.perform(get("/rooms/hotels/{hotelId}/recommend", hotelId)
                        .param("checkIn", "2026-12-01")
                        .param("checkOut", "2026-12-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timesBooked").value(1))
                .andExpect(jsonPath("$[1].timesBooked").value(3));
    }

    @Test
    void recommendRooms_shouldReturn409WhenNoneAvailable() throws Exception {
        when(roomService.recommendRooms(eq(hotelId), eq(LocalDate.of(2026, 12, 1)), eq(LocalDate.of(2026, 12, 5))))
                .thenThrow(new IllegalStateException("No rooms available for the requested dates"));

        mockMvc.perform(get("/rooms/hotels/{hotelId}/recommend", hotelId)
                        .param("checkIn", "2026-12-01")
                        .param("checkOut", "2026-12-05"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ROOM_NOT_AVAILABLE"));
    }

    @Test
    void deleteRoom_shouldReturn204() throws Exception {
        doNothing().when(roomService).deleteRoom(roomId);

        mockMvc.perform(delete("/rooms/{roomId}", roomId))
                .andExpect(status().isNoContent());
    }

    // --- Internal Controller ---

    @Test
    void confirmAvailability_shouldReturnLockInfo() throws Exception {
        UUID requestId = UUID.randomUUID();
        RoomLock lock = new RoomLock(roomId, requestId,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                RoomLockStatus.HELD, LocalDateTime.now().plusMinutes(2));
        lock.setId(UUID.randomUUID());

        when(roomService.confirmAvailability(eq(roomId), eq(requestId), any()))
                .thenReturn(lock);

        String body = "{\"bookingId\":null,\"checkIn\":\"2026-12-01\",\"checkOut\":\"2026-12-05\"}";

        mockMvc.perform(post("/internal/rooms/{roomId}/confirm-availability", roomId)
                        .param("requestId", requestId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    void releaseRoomLock_shouldReturnSuccess() throws Exception {
        UUID requestId = UUID.randomUUID();
        ReleaseLockRequest req = new ReleaseLockRequest();
        req.setRequestId(requestId);

        doNothing().when(roomService).releaseRoomLock(eq(roomId), eq(requestId));

        mockMvc.perform(post("/internal/rooms/{roomId}/release", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("released"));
    }

    @Test
    void confirmBooking_shouldReturnSuccess() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        doNothing().when(roomService).confirmBooking(eq(roomId), eq(requestId), eq(bookingId));

        mockMvc.perform(post("/internal/rooms/{roomId}/confirm-booking", roomId)
                        .param("requestId", requestId.toString())
                        .param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmed"));
    }

    @Test
    void cancelBooking_shouldReturnSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();

        doNothing().when(roomService).cancelBooking(eq(roomId), eq(bookingId));

        mockMvc.perform(post("/internal/rooms/{roomId}/cancel-booking", roomId)
                        .param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
    }
}
