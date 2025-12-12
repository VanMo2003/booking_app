package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface BookingService {
    BookingResponse create(BookingCreateRequest req);
    BookingResponse update(Long id, BookingUpdateRequest req);
    BookingResponse getById(Long id);
    PageResponse<BookingResponse> getAll(int page, int size);
    void delete(Long id);
}
