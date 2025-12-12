package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.RoomTypeCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomTypeUpdateRequest;
import com.vanmo.booking_app_be.dto.response.RoomTypeResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface RoomTypeService {
    RoomTypeResponse create(RoomTypeCreateRequest req);
    RoomTypeResponse update(Long id, RoomTypeUpdateRequest req);
    RoomTypeResponse getById(Long id);
    PageResponse<RoomTypeResponse> getAll(int page, int size);
    void delete(Long id);
}
