package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
