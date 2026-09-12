package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByAccount_Id(String accountId);

    List<Customer> findAllByPhoneNumber(String phoneNumber);

    /**
     * Walk-in profiles only - used when a returning guest self-registers and claims their profile.
     * A list rather than Optional because nothing stops two walk-ins sharing a phone number.
     */
    List<Customer> findAllByPhoneNumberAndAccountIsNull(String phoneNumber);
}
