package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
