package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByHotel_Id(Long hotelId);
}
