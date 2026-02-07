package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.BookingService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingServiceRepository extends JpaRepository<BookingService, Long> {
}
