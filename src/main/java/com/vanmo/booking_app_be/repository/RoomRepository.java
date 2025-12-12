package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
