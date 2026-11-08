package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.HotelRequest;
import com.hotelbooking.hotel.dto.HotelResponse;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService hotelService;

    private UUID hotelId;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        hotelId = UUID.randomUUID();
        hotel = new Hotel("Grand Hotel", "Luxury", "123 Main St", "New York");
        hotel.setId(hotelId);
    }

    @Test
    void createHotel_shouldReturnResponse() {
        HotelRequest request = new HotelRequest();
        request.setName("Grand Hotel");
        request.setDescription("Luxury");
        request.setAddress("123 Main St");
        request.setCity("New York");

        when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);

        HotelResponse response = hotelService.createHotel(request);

        assertThat(response.getName()).isEqualTo("Grand Hotel");
        assertThat(response.getCity()).isEqualTo("New York");
        verify(hotelRepository).save(any(Hotel.class));
    }

    @Test
    void createHotel_shouldGenerateUUID() {
        HotelRequest request = new HotelRequest();
        request.setName("Test Hotel");
        request.setAddress("1 Test St");
        request.setCity("Test City");

        when(hotelRepository.save(any(Hotel.class))).thenAnswer(invocation -> {
            Hotel h = invocation.getArgument(0);
            assertThat(h.getId()).isNotNull();
            return h;
        });

        HotelResponse response = hotelService.createHotel(request);
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void getAllHotels_shouldReturnAll() {
        when(hotelRepository.findAll()).thenReturn(List.of(hotel));

        List<HotelResponse> responses = hotelService.getAllHotels();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Grand Hotel");
    }

    @Test
    void getHotelById_shouldReturnHotel() {
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));

        HotelResponse response = hotelService.getHotelById(hotelId);

        assertThat(response.getId()).isEqualTo(hotelId);
        assertThat(response.getName()).isEqualTo("Grand Hotel");
    }

    @Test
    void getHotelById_shouldThrowWhenNotFound() {
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.getHotelById(hotelId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hotel not found");
    }

    @Test
    void updateHotel_shouldUpdateFields() {
        HotelRequest request = new HotelRequest();
        request.setName("Updated Hotel");
        request.setCity("London");

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);

        HotelResponse response = hotelService.updateHotel(hotelId, request);

        assertThat(response.getName()).isEqualTo("Updated Hotel");
        assertThat(response.getCity()).isEqualTo("London");
    }

    @Test
    void updateHotel_shouldThrowWhenNotFound() {
        HotelRequest request = new HotelRequest();
        request.setName("X");

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.updateHotel(hotelId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteHotel_shouldDelete() {
        when(hotelRepository.existsById(hotelId)).thenReturn(true);

        hotelService.deleteHotel(hotelId);

        verify(hotelRepository).deleteById(hotelId);
    }

    @Test
    void deleteHotel_shouldThrowWhenNotFound() {
        when(hotelRepository.existsById(hotelId)).thenReturn(false);

        assertThatThrownBy(() -> hotelService.deleteHotel(hotelId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hotel not found");
    }
}
