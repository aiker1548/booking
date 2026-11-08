package com.hotelbooking.hotel.config;

import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Value("${app.demo-data.enabled:true}")
    private boolean enabled;

    public DemoDataInitializer(
            HotelRepository hotelRepository,
            RoomRepository roomRepository
    ) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        seedHotel("Demo Distribution Hotel", "Hotel for room distribution demonstration",
                "1 Demo Street", "Vilnius", 301);
        seedHotel("Demo Riverside Hotel", "Hotel near the river",
                "2 River Street", "Kaunas", 401);
        seedHotel("Demo Central Hotel", "Central city hotel",
                "3 Central Avenue", "Riga", 501);
        seedHotel("Demo Mountain Hotel", "Mountain resort hotel",
                "4 Mountain Road", "Tallinn", 601);
        seedHotel("Demo Business Hotel", "Business hotel",
                "5 Business Avenue", "Warsaw", 701);
    }

    private void seedHotel(
            String name,
            String description,
            String address,
            String city,
            int firstRoomNumber
    ) {
        Hotel hotel = hotelRepository.findAll().stream()
                .filter(existing -> name.equals(existing.getName()))
                .findFirst()
                .orElseGet(() -> hotelRepository.save(
                        new Hotel(name, description, address, city)
                ));

        int[] bookingStatistics = {0, 1, 2, 3, 5, 7, 10, 15};

        for (int i = 0; i < bookingStatistics.length; i++) {
            String roomNumber = String.valueOf(firstRoomNumber + i);

            if (roomRepository.findByHotelIdAndNumber(
                    hotel.getId(), roomNumber
            ).isPresent()) {
                continue;
            }

            Room room = new Room(hotel.getId(), roomNumber);
            room.setTimesBooked(bookingStatistics[i]);
            roomRepository.save(room);
        }
    }
}
