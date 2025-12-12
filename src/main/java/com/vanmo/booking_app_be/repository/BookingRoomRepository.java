package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
}
