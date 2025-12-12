package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Long> {
}
