package com.vanmo.booking_app_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.HotelChain;

public interface HotelChainRepository extends JpaRepository<HotelChain, Long> {
    HotelChain findByAccount_Id(String accountId);
}
