package com.vanmo.booking_app_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByAccount_Id(String accountId);
}
