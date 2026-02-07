package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {
}
