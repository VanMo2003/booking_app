package com.vanmo.booking_app_be.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.RevenueByServiceDTO;
import com.vanmo.booking_app_be.entity.BookingService;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;

public interface BookingServiceRepository extends JpaRepository<BookingService, Long> {

    /** Revenue per add-on service; BookingService.total is already a line total. */
    @Query(
            """
		select new com.vanmo.booking_app_be.dto.response.RevenueByServiceDTO(
			s.id, s.name, sum(bs.quantity), sum(bs.total)
		)
		from BookingService bs
		join bs.booking b
		join bs.service s
		where b.hotel.id in :hotelIds
		and b.bookingStatus = :status
		and b.checkoutDate between :from and :to
		group by s.id, s.name
		order by sum(bs.total) desc
	""")
    List<RevenueByServiceDTO> revenueByService(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
