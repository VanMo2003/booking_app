package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
    @Query("""
        select distinct br.room.id
        from BookingRoom br
        join br.booking b
        where b.bookingStatus in ('PENDING', 'CONFIRMED')
          and b.checkinDate < :checkout
          and b.checkoutDate > :checkin
    """)
    List<Long> findBookedRoomIds(
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout
    );

    @Query("""
        select count(br) > 0
        from BookingRoom br
        join br.booking b
        where br.room.id = :roomId
          and b.bookingStatus in ('PENDING', 'CONFIRMED')
          and b.checkinDate < :checkout
          and b.checkoutDate > :checkin
    """)
    boolean existsRoomBooked(
            @Param("roomId") Long roomId,
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout
    );
}
