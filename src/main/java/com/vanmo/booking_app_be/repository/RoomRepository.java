package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByHotelAndRoomNumber(Hotel hotel, @NotNull String roomNumber);

    Page<Room> findByHotelId(Long hotelId, Pageable pageable);
}
