package com.vanmo.booking_app_be.repository;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findAllByCategory(@NotNull String category);

    List<Hotel> findAllByAccount_Id(String accountId);

    List<Hotel> findAllByHotelChain_Id(Long hotelChainId);
}
