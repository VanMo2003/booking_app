package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Service;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByHotelId(Long hotelId);

    List<Service> findAllByHotel_Id(Long hotelId);
}
