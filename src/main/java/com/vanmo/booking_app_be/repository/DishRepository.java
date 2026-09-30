package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Dish;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findAllByHotel_Id(Long hotelId);
}
