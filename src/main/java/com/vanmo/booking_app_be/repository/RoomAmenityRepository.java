package com.vanmo.booking_app_be.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.RoomAmenity;

public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, Long> {

    List<RoomAmenity> findAllByRoomId(Long roomId);

    List<RoomAmenity> findAllByAmenity(Amenity amenity);
}
