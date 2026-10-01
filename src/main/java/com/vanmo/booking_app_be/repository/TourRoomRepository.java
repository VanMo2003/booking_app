package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vanmo.booking_app_be.entity.TourRoom;

@Repository
public interface TourRoomRepository extends JpaRepository<TourRoom, Long> {
    List<TourRoom> findAllByTour_Id(Long tourId);

    List<TourRoom> findAllByTour_Hotel_Id(Long hotelId);
}
