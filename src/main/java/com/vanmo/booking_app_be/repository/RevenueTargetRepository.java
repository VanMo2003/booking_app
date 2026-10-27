package com.vanmo.booking_app_be.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.RevenueTarget;

public interface RevenueTargetRepository extends JpaRepository<RevenueTarget, Long> {
    Optional<RevenueTarget> findByHotel_IdAndMonth(Long hotelId, LocalDate month);

    List<RevenueTarget> findAllByMonth(LocalDate month);
}
