package com.vanmo.booking_app_be.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.HotelReviewDigest;

public interface HotelReviewDigestRepository extends JpaRepository<HotelReviewDigest, Long> {

    Optional<HotelReviewDigest> findByHotel_Id(Long hotelId);
}
