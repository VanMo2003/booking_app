package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}
