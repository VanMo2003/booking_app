package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findAllByHotelId(Long hotelId);

    Employee findByAccount_Id(String accountId);
}
