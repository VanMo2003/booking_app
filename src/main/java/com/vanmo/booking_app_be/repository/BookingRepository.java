package com.vanmo.booking_app_be.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.RevenueByDateDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByMonthDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByYearDTO;
import com.vanmo.booking_app_be.dto.response.StaffPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.TopCustomerDTO;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByHotel_Id(Long hotelId);

    @Query("select distinct b.customer from Booking b where b.hotel.id = :hotelId")
    List<Customer> findDistinctCustomersByHotelId(@Param("hotelId") Long hotelId);

    List<Booking> findAllByCustomer_Id(Long customerId);

    List<Booking> findAllByCustomer_IdAndBookingStatus(Long customerId, @NotNull BookingStatus bookingStatus);

    List<Booking> findByPaymentStatusAndPaymentExpireAtBefore(PaymentStatus paymentStatus, LocalDateTime time);

    @Query(
            value =
                    """
		SELECT new com.vanmo.booking_app_be.dto.response.RevenueByDateDTO(
			b.checkoutDate,
			SUM(b.totalAmount),
			COUNT(b.id)
		)
		FROM Booking b
		WHERE b.hotel.id = :hotelId
		AND b.bookingStatus = :status
		AND b.checkoutDate BETWEEN :from AND :to
		GROUP BY b.checkoutDate
		ORDER BY b.checkoutDate
	""")
    List<RevenueByDateDTO> revenueByDate(
            @Param("hotelId") Long hotelId,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(
            value =
                    """
		SELECT new com.vanmo.booking_app_be.dto.response.RevenueByMonthDTO(
			YEAR(b.checkoutDate),
			MONTH(b.checkoutDate),
			SUM(b.totalAmount),
			COUNT(b.id)
		)
		FROM Booking b
		WHERE b.hotel.id = :hotelId
		AND b.bookingStatus = :status
		AND YEAR(b.checkoutDate) = :year
		GROUP BY YEAR(b.checkoutDate), MONTH(b.checkoutDate)
		ORDER BY MONTH(b.checkoutDate)
	""")
    List<RevenueByMonthDTO> revenueByMonth(
            @Param("hotelId") Long hotelId, @Param("status") BookingStatus status, @Param("year") Integer year);

    @Query(
            value =
                    """
		SELECT new com.vanmo.booking_app_be.dto.response.RevenueByYearDTO(
			YEAR(b.checkoutDate),
			SUM(b.totalAmount),
			COUNT(b.id)
		)
		FROM Booking b
		WHERE b.hotel.id = :hotelId
		AND b.bookingStatus = :status
		GROUP BY YEAR(b.checkoutDate)
		ORDER BY YEAR(b.checkoutDate)
	""")
    List<RevenueByYearDTO> revenueByYear(@Param("hotelId") Long hotelId, @Param("status") BookingStatus status);

    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.RevenueByDateDTO(
			b.checkoutDate,
			SUM(b.totalAmount),
			COUNT(b.id)
		)
		FROM Booking b
		WHERE b.hotel.id = :hotelId
		AND b.bookingStatus = :status
		AND b.checkoutDate BETWEEN :from AND :to
		GROUP BY b.checkoutDate
	""")
    List<RevenueByDateDTO> revenueByDateRange(
            @Param("hotelId") Long hotelId,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(
            value =
                    """
		SELECT new com.vanmo.booking_app_be.dto.response.RevenueByDateDTO(
			b.checkoutDate,
			SUM(b.totalAmount),
			COUNT(b.id)
		)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkoutDate BETWEEN :from AND :to
		GROUP BY b.checkoutDate
		ORDER BY b.checkoutDate
	""")
    List<RevenueByDateDTO> revenueByDateForHotels(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(
            value =
                    """
		SELECT new com.vanmo.booking_app_be.dto.response.RevenueByMonthDTO(
			YEAR(b.checkoutDate),
			MONTH(b.checkoutDate),
			SUM(b.totalAmount),
			COUNT(b.id)
		)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND YEAR(b.checkoutDate) = :year
		GROUP BY YEAR(b.checkoutDate), MONTH(b.checkoutDate)
		ORDER BY MONTH(b.checkoutDate)
	""")
    List<RevenueByMonthDTO> revenueByMonthForHotels(
            @Param("hotelIds") List<Long> hotelIds, @Param("status") BookingStatus status, @Param("year") Integer year);

    @Query(
            value =
                    """
		SELECT new com.vanmo.booking_app_be.dto.response.RevenueByYearDTO(
			YEAR(b.checkoutDate),
			SUM(b.totalAmount),
			COUNT(b.id)
		)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		GROUP BY YEAR(b.checkoutDate)
		ORDER BY YEAR(b.checkoutDate)
	""")
    List<RevenueByYearDTO> revenueByYearForHotels(
            @Param("hotelIds") List<Long> hotelIds, @Param("status") BookingStatus status);

    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.RevenueByDateDTO(
			b.checkoutDate,
			SUM(b.totalAmount),
			COUNT(b.id)
		)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkoutDate BETWEEN :from AND :to
		GROUP BY b.checkoutDate
	""")
    List<RevenueByDateDTO> revenueByDateRangeForHotels(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.TopCustomerDTO(
			c.id, c.fullName, c.phoneNumber, COUNT(b.id), SUM(b.totalAmount)
		)
		FROM Booking b
		JOIN b.customer c
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkoutDate BETWEEN :from AND :to
		GROUP BY c.id, c.fullName, c.phoneNumber
		ORDER BY SUM(b.totalAmount) DESC
	""")
    List<TopCustomerDTO> topCustomers(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    /**
     * Bookings grouped by the staff/manager account that took them. Customer self-service bookings
     * have a null handledBy and are excluded.
     */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.StaffPerformanceDTO(
			acc.id, acc.username, null, null, COUNT(b.id), SUM(b.totalAmount)
		)
		FROM Booking b
		JOIN b.handledBy acc
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus <> com.vanmo.booking_app_be.entity.enums.BookingStatus.CANCELED
		AND b.checkinDate BETWEEN :from AND :to
		GROUP BY acc.id, acc.username
		ORDER BY COUNT(b.id) DESC
	""")
    List<StaffPerformanceDTO> staffPerformance(
            @Param("hotelIds") List<Long> hotelIds, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            """
		SELECT COUNT(b.id)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.checkinDate BETWEEN :from AND :to
	""")
    long countBookings(
            @Param("hotelIds") List<Long> hotelIds, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            """
		SELECT COUNT(b.id)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkinDate BETWEEN :from AND :to
	""")
    long countBookingsByStatus(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
