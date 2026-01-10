package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
    List<HotelImage> findAllByHotel_Id(Long hotelId);
}
