package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.RoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomDetailResponse;
import com.vanmo.booking_app_be.dto.response.RoomResponse;

public interface RoomService {
    RoomResponse create(RoomCreateRequest req);
    RoomResponse update(Long id, RoomUpdateRequest req);
    RoomDetailResponse getById(Long id);
    PageResponse<RoomResponse> getAllByHotel(Long hotelId, int page, int size);
    void delete(Long id);
}
