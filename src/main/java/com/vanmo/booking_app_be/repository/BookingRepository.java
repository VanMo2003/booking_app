package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.dto.response.RevenueByDateDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByMonthDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByYearDTO;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByHotel_Id(Long hotelId);

    List<Booking> findAllByCustomer_Id(Long customerId);

    List<Booking> findAllByCustomer_IdAndBookingStatus(Long customerId, @NotNull BookingStatus bookingStatus);

    List<Booking> findByPaymentStatusAndPaymentExpireAtBefore(
            PaymentStatus paymentStatus,
            LocalDateTime time
    );

    @Query(value = """
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
            @Param("to") LocalDate to
    );

    @Query(value = """
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
            @Param("hotelId") Long hotelId,
            @Param("status") BookingStatus status,
            @Param("year") Integer year
    );

    @Query(value = """
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
    List<RevenueByYearDTO> revenueByYear(
            @Param("hotelId") Long hotelId,
            @Param("status") BookingStatus status
    );

    @Query("""
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
            @Param("to") LocalDate to
    );
}
