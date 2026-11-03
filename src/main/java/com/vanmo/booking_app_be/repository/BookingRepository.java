package com.vanmo.booking_app_be.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.CanceledBookingRow;
import com.vanmo.booking_app_be.dto.response.CollectionRow;
import com.vanmo.booking_app_be.dto.response.CompletedBookingRow;
import com.vanmo.booking_app_be.dto.response.GuestOriginRow;
import com.vanmo.booking_app_be.dto.response.OpenBookingRow;
import com.vanmo.booking_app_be.dto.response.PaymentTimingRow;
import com.vanmo.booking_app_be.dto.response.ReceivableDTO;
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

    /** Payments of the branches' bookings recorded in [start, end), whatever the booking's status now. */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.CollectionRow(b.paidAt, b.paymentMethod, b.totalAmount)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.paymentStatus = com.vanmo.booking_app_be.entity.enums.PaymentStatus.PAID
		AND b.paidAt >= :start AND b.paidAt < :end
	""")
    List<CollectionRow> findCollections(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Bookings in a status checking out during [from, to], with when their payment was recorded. */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.PaymentTimingRow(
			b.id, b.totalAmount, b.paymentStatus, b.paidAt, b.checkinDate, b.checkoutDate
		)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkoutDate BETWEEN :from AND :to
	""")
    List<PaymentTimingRow> findPaymentTiming(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Bookings in a status checking out during [from, to] with their guest's hometown. */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.GuestOriginRow(c.id, c.hometown, b.totalAmount)
		FROM Booking b JOIN b.customer c
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkoutDate BETWEEN :from AND :to
	""")
    List<GuestOriginRow> findGuestOrigins(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Cancelled bookings checking in during [from, to], with when and by which side they were cancelled. */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.CanceledBookingRow(
			b.id, b.totalAmount, b.checkinDate, b.canceledAt, b.onUpdate, b.canceledBy
		)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = com.vanmo.booking_app_be.entity.enums.BookingStatus.CANCELED
		AND b.checkinDate BETWEEN :from AND :to
	""")
    List<CanceledBookingRow> findCanceled(
            @Param("hotelIds") List<Long> hotelIds, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Bookings in a status checking in during [from, to] that the front desk took (handledBy set). */
    @Query(
            """
		SELECT COUNT(b.id)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkinDate BETWEEN :from AND :to
		AND b.handledBy IS NOT NULL
	""")
    long countTakenAtDeskByStatus(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Customers with a booking in the status that checked out before :before - who counts as a returning guest. */
    @Query(
            """
		SELECT DISTINCT b.customer.id
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkoutDate < :before
	""")
    List<Long> findCustomerIdsBefore(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("before") LocalDate before);

    /** What the bookings in a status checking in during [from, to] were worth - e.g. revenue lost to cancellations. */
    @Query(
            """
		SELECT COALESCE(SUM(b.totalAmount), 0)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkinDate BETWEEN :from AND :to
	""")
    BigDecimal sumAmountByStatus(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

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

    /** Bookings in a status whose check-out falls in [from, to] - the raw rows of the revenue summary. */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.CompletedBookingRow(
			b.id, b.totalAmount, b.paymentMethod, acc.id, b.checkinDate, b.checkoutDate, b.onCreate, b.customer.id
		)
		FROM Booking b
		LEFT JOIN b.handledBy acc
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.checkoutDate BETWEEN :from AND :to
	""")
    List<CompletedBookingRow> findSales(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Bookings in one of the statuses checking out on or before :to - open stays still to earn, including the overdue
     * ones whose check-out day already passed.
     */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.OpenBookingRow(
			b.id, b.totalAmount, b.bookingStatus, b.paymentStatus, b.checkoutDate
		)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus IN :statuses
		AND b.checkoutDate <= :to
	""")
    List<OpenBookingRow> findOpen(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("to") LocalDate to);

    /** The same bookings as {@link #findUnpaid}, with who and where, oldest check-out first. */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.ReceivableDTO(
			b.id, h.id, h.name, c.fullName, c.phoneNumber, b.checkoutDate, b.totalAmount, b.paymentMethod
		)
		FROM Booking b
		JOIN b.hotel h
		JOIN b.customer c
		WHERE h.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.paymentStatus <> :paid
		ORDER BY b.checkoutDate, b.id
	""")
    List<ReceivableDTO> findReceivables(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("paid") PaymentStatus paid);

    /** Bookings in a status whose payment was never recorded as :paid. */
    @Query(
            """
		SELECT new com.vanmo.booking_app_be.dto.response.OpenBookingRow(
			b.id, b.totalAmount, b.bookingStatus, b.paymentStatus, b.checkoutDate
		)
		FROM Booking b
		WHERE b.hotel.id IN :hotelIds
		AND b.bookingStatus = :status
		AND b.paymentStatus <> :paid
	""")
    List<OpenBookingRow> findUnpaid(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("status") BookingStatus status,
            @Param("paid") PaymentStatus paid);
}
