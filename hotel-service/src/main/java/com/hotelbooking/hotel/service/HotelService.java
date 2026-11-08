package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.HotelRequest;
import com.hotelbooking.hotel.dto.HotelResponse;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.repository.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        log.info("Creating hotel: {}", request.getName());

        Hotel hotel = new Hotel(request.getName(), request.getDescription(),
                request.getAddress(), request.getCity());
        Hotel saved = hotelRepository.save(hotel);

        log.info("Hotel created with id: {}", saved.getId());
        return toResponse(saved);
    }

    public List<HotelResponse> getAllHotels() {
        return hotelRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public HotelResponse getHotelById(UUID hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + hotelId));
        return toResponse(hotel);
    }

    @Transactional
    public HotelResponse updateHotel(UUID hotelId, HotelRequest request) {
        log.info("Updating hotel: {}", hotelId);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + hotelId));

        if (request.getName() != null) {
            hotel.setName(request.getName());
        }
        if (request.getDescription() != null) {
            hotel.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            hotel.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            hotel.setCity(request.getCity());
        }

        Hotel updated = hotelRepository.save(hotel);
        log.info("Hotel updated: {}", hotelId);
        return toResponse(updated);
    }

    @Transactional
    public void deleteHotel(UUID hotelId) {
        log.info("Deleting hotel: {}", hotelId);

        if (!hotelRepository.existsById(hotelId)) {
            throw new IllegalArgumentException("Hotel not found: " + hotelId);
        }
        hotelRepository.deleteById(hotelId);
        log.info("Hotel deleted: {}", hotelId);
    }

    private HotelResponse toResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(), hotel.getName(), hotel.getDescription(),
                hotel.getAddress(), hotel.getCity(),
                hotel.getCreatedAt(), hotel.getUpdatedAt());
    }
}
