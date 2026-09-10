package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.RoomImage;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findAllByRoom_Id(Long roomId);
}
