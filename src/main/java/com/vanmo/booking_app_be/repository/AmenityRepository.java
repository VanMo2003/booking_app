package com.vanmo.booking_app_be.repository;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.entity.Amenity;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findByHotelIdAndCommon(Long hotelId, boolean common);

    @Query(
            value =
                    """
		select *  from amenity
		where (amenity.room_id = :roomId and amenity.common = false) or (amenity.hotel_id = :hotelId)
		order by amenity.common asc
	""",
            nativeQuery = true)
    List<Amenity> getAllAmenityByRoom(@Param("roomId") Long roomId, @Param("hotelId") Long hotelId);

    Amenity findByHotelIdAndName(Long hotelId, @NotNull String name);

    List<Amenity> findAllByHotel_Id(Long hotelId);
}
