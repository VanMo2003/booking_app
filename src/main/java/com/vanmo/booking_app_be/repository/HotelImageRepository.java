package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
}
