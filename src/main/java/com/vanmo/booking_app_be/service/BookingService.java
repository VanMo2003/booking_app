package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.dto.response.ListBookingResponse;

import java.util.List;

public interface BookingService {
    BookingResponse create(BookingCreateRequest req);
    BookingResponse update(Long id, BookingUpdateRequest req);
    BookingResponse getById(Long id);
    List<ListBookingResponse> getAllByHotel(Long hotelId, String bookingStatus);
    List<ListBookingResponse> getAllByCustomer(Long customerId, String bookingStatus);
    void delete(Long id);
    BookingResponse confirmBooking(Long id);
    BookingResponse cancelBooking(Long id);
    BookingResponse completeBooking(Long id);
}
