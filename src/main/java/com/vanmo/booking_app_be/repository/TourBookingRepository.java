package com.vanmo.booking_app_be.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.entity.TourBooking;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;

@Repository
public interface TourBookingRepository extends JpaRepository<TourBooking, Long> {
    List<TourBooking> findAllByHotel_IdOrderByIdDesc(Long hotelId);

    List<TourBooking> findAllByHotel_IdAndStatusOrderByIdDesc(Long hotelId, TourBookingStatus status);

    List<TourBooking> findAllByCustomer_IdOrderByIdDesc(Long customerId);

    List<TourBooking> findByCustomer_IdAndHotel_IdOrderByIdDesc(Long customerId, Long hotelId, Pageable pageable);

    Optional<TourBooking> findFirstByCustomer_IdAndTour_IdAndTourDateAndStatusIn(
            Long customerId, Long tourId, LocalDate tourDate, Collection<TourBookingStatus> statuses);

    Optional<TourBooking> findFirstByBooking_IdAndStatusIn(Long bookingId, Collection<TourBookingStatus> statuses);

    /** Seats already taken on one departure. */
    @Query("select coalesce(sum(b.guests), 0) from TourBooking b "
            + "where b.tour.id = :tourId and b.tourDate = :tourDate and b.status in :statuses")
    int sumGuests(
            @Param("tourId") Long tourId,
            @Param("tourDate") LocalDate tourDate,
            @Param("statuses") Collection<TourBookingStatus> statuses);

    /** Tour orders in a status whose tour date falls in [from, to], for the revenue reports. */
    @Query("select new com.vanmo.booking_app_be.dto.response.TourSaleRow("
            + "tr.id, b.tourName, b.guests, b.tourAmount, b.roomAmount, b.totalAmount, b.createdByAi) "
            + "from TourBooking b left join b.tour tr "
            + "where b.hotel.id in :hotelIds and b.status = :status and b.tourDate between :from and :to")
    List<TourSaleRow> findSales(
            @Param("hotelIds") Collection<Long> hotelIds,
            @Param("status") TourBookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
