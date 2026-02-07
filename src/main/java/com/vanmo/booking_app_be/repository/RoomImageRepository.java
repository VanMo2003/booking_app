package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.HotelImage;
import com.vanmo.booking_app_be.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findAllByRoom_Id(Long roomId);
}
