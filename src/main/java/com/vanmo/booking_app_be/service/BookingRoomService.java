package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.BookingRoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingRoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingRoomResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface BookingRoomService {
    BookingRoomResponse create(BookingRoomCreateRequest req);

    BookingRoomResponse update(Long id, BookingRoomUpdateRequest req);

    BookingRoomResponse getById(Long id);

    PageResponse<BookingRoomResponse> getAll(int page, int size);

    void delete(Long id);
}
