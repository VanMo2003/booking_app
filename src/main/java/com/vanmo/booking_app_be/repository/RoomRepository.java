package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByHotelAndRoomNumber(Hotel hotel, @NotNull String roomNumber);

    Page<Room> findByHotelId(Long hotelId, Pageable pageable);

    List<Room> findAllByHotel_Id(Long hotelId);
}
