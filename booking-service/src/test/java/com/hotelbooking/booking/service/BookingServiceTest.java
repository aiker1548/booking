package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.dto.BookingResponse;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private BookingRequest bookingRequest;
    private Booking booking;
    private UUID userId;
    private UUID bookingId;
    private UUID roomId;
    private UUID hotelId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        hotelId = UUID.randomUUID();

        bookingRequest = new BookingRequest();
        bookingRequest.setUserId(userId);
        bookingRequest.setHotelId(hotelId);
        bookingRequest.setRoomId(roomId);
        bookingRequest.setStartDate(LocalDate.now());
        bookingRequest.setEndDate(LocalDate.now().plusDays(1));

        booking = new Booking(
                bookingId,
                userId,
                hotelId,
                roomId,
                bookingRequest.getStartDate(),
                bookingRequest.getEndDate(),
                BookingStatus.PENDING,
                UUID.randomUUID()
        );
    }

    @Test
    void createBooking_ShouldCreateNewBooking() {
        when(bookingRepository.existsByUserIdAndRoomIdAndStartDateAndEndDate(
                any(UUID.class), any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        Booking createdBooking = bookingService.createBooking(bookingRequest);

        assertNotNull(createdBooking);
        assertEquals(bookingId, createdBooking.getBookingId());
        assertEquals(BookingStatus.PENDING, createdBooking.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_ShouldThrowExceptionWhenBookingExists() {
        when(bookingRepository.existsByUserIdAndRoomIdAndStartDateAndEndDate(
                any(UUID.class), any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(bookingRequest));
    }

    @Test
    void confirmBooking_ShouldConfirmPendingBooking() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        bookingService.confirmBooking(bookingId, roomId, bookingRequest.getStartDate(), bookingRequest.getEndDate());

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void confirmBooking_ShouldThrowExceptionWhenBookingNotFound() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bookingService.confirmBooking(bookingId, roomId, bookingRequest.getStartDate(), bookingRequest.getEndDate()));
    }

    @Test
    void cancelBooking_ShouldCancelPendingBooking() {
        when(bookingRepository.findByBookingIdAndUserId(bookingId, userId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        bookingService.cancelBooking(bookingId, userId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void cancelBooking_ShouldThrowExceptionWhenBookingNotFound() {
        when(bookingRepository.findByBookingIdAndUserId(bookingId, userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bookingService.cancelBooking(bookingId, userId));
    }

    @Test
    void getBooking_ShouldReturnBookingForUser() {
        when(bookingRepository.findByBookingIdAndUserId(bookingId, userId)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.getBooking(bookingId, userId);

        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        assertEquals(BookingStatus.PENDING, response.getStatus());
    }

    @Test
    void getBooking_ShouldThrowExceptionWhenBookingNotFound() {
        when(bookingRepository.findByBookingIdAndUserId(bookingId, userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bookingService.getBooking(bookingId, userId));
    }

    @Test
    void getUserBookings_ShouldReturnListOfBookings() {
        when(bookingRepository.findByUserId(userId)).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.getUserBookings(userId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(bookingId, responses.get(0).getBookingId());
    }
}
