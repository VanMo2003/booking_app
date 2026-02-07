package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByAccount_Id(String accountId);
}
