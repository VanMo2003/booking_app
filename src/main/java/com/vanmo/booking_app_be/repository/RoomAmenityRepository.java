package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.RoomAmenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, Long> {

    List<RoomAmenity> findAllByRoomId(Long roomId);

    List<RoomAmenity> findAllByAmenity(Amenity amenity);
}
