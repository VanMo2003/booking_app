package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByHotel_Id(Long hotelId);

    List<Booking> findAllByCustomer_Id(Long customerId);

    List<Booking> findAllByCustomer_IdAndBookingStatus(Long customerId, @NotNull BookingStatus bookingStatus);
}
