package com.vanmo.booking_app_be.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.BookedRoomNightRow;
import com.vanmo.booking_app_be.entity.BookingRoom;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
    @Query(
            """
		select distinct br.room.id
		from BookingRoom br
		join br.booking b
		where b.bookingStatus in ('PENDING', 'CONFIRMED')
		and b.checkinDate < :checkout
		and b.checkoutDate > :checkin
	""")
    List<Long> findBookedRoomIds(@Param("checkin") LocalDate checkin, @Param("checkout") LocalDate checkout);

    @Query(
            """
		select count(br) > 0
		from BookingRoom br
		join br.booking b
		where br.room.id = :roomId
		and b.bookingStatus in ('PENDING', 'CONFIRMED')
		and b.checkinDate < :checkout
		and b.checkoutDate > :checkin
	""")
    boolean existsRoomBooked(
            @Param("roomId") Long roomId, @Param("checkin") LocalDate checkin, @Param("checkout") LocalDate checkout);

    /**
     * Every booked room whose stay overlaps [from, to) at the given branches, excluding cancelled
     * bookings. Feeds the occupancy report (count distinct rooms per night).
     */
    @Query(
            """
		select new com.vanmo.booking_app_be.dto.response.BookedRoomNightRow(
			r.id, rt.id, rt.name, r.price, b.checkinDate, b.checkoutDate
		)
		from BookingRoom br
		join br.booking b
		join br.room r
		join r.roomType rt
		where b.hotel.id in :hotelIds
		and b.bookingStatus <> com.vanmo.booking_app_be.entity.enums.BookingStatus.CANCELED
		and b.checkinDate < :to
		and b.checkoutDate > :from
	""")
    List<BookedRoomNightRow> findBookedRoomNights(
            @Param("hotelIds") List<Long> hotelIds, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Same shape, with the booking and its total, for the bookings in a status checking out in [from, to] - what the
     * revenue by room type and by room share out (see RevenueFigures.roomRevenue).
     */
    @Query(
            """
		select new com.vanmo.booking_app_be.dto.response.BookedRoomNightRow(
			r.id, rt.id, rt.name, r.price, b.checkinDate, b.checkoutDate, b.id, b.totalAmount
		)
		from BookingRoom br
		join br.booking b
		join br.room r
		join r.roomType rt
		where b.hotel.id in :hotelIds
		and b.bookingStatus = :status
		and b.checkoutDate between :from and :to
	""")
    List<BookedRoomNightRow> findRoomNightsByStatus(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
