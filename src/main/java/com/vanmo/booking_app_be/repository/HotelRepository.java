package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.dto.response.HotelResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Hotel;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findAllByCategory(@NotNull String category);

    Hotel findByAccount_Id(String accountId);
}
