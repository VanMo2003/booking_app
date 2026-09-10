package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.BookingServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingServiceResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface BookingServiceService {
    BookingServiceResponse create(BookingServiceCreateRequest req);

    BookingServiceResponse update(Long id, BookingServiceUpdateRequest req);

    BookingServiceResponse getById(Long id);

    PageResponse<BookingServiceResponse> getAll(int page, int size);

    void delete(Long id);
}
