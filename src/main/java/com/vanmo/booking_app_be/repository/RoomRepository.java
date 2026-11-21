package com.vanmo.booking_app_be.repository;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.RoomTypeStockRow;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByHotelAndRoomNumber(Hotel hotel, @NotNull String roomNumber);

    Page<Room> findByHotelId(Long hotelId, Pageable pageable);

    List<Room> findAllByHotel_Id(Long hotelId);

    long countByHotel_IdIn(List<Long> hotelIds);

    /** Rooms of the branches per room type, with their average list price, for the room-type report. */
    @Query("select new com.vanmo.booking_app_be.dto.response.RoomTypeStockRow(rt.id, rt.name, count(r), avg(r.price)) "
            + "from Room r join r.roomType rt where r.hotel.id in :hotelIds group by rt.id, rt.name")
    List<RoomTypeStockRow> countByRoomType(@Param("hotelIds") List<Long> hotelIds);
}
