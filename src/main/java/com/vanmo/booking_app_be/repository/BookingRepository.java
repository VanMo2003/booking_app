package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
