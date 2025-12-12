package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {
}
