package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.dto.response.ServiceResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByHotelId(Long hotelId);

    List<Service> findAllByHotel_Id(Long hotelId);
}
