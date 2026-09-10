package com.vanmo.booking_app_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {}
