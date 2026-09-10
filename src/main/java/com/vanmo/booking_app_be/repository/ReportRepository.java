package com.vanmo.booking_app_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {}
