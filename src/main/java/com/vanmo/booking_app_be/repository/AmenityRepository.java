package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}
