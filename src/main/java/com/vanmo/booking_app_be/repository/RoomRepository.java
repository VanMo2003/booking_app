package com.vanmo.booking_app_be.repository;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByHotelAndRoomNumber(Hotel hotel, @NotNull String roomNumber);

    Page<Room> findByHotelId(Long hotelId, Pageable pageable);

    List<Room> findAllByHotel_Id(Long hotelId);

    long countByHotel_IdIn(List<Long> hotelIds);
}
