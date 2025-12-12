package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
