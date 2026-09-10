package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.HotelImage;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
    List<HotelImage> findAllByHotel_Id(Long hotelId);
}
