package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Tour;

public interface TourRepository extends JpaRepository<Tour, Long> {
    List<Tour> findAllByHotel_Id(Long hotelId);
}
