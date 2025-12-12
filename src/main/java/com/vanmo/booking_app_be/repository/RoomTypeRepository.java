package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
}
