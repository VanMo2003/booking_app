package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.RoomAmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomAmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.RoomAmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface RoomAmenityService {
    RoomAmenityResponse create(RoomAmenityCreateRequest req);
    RoomAmenityResponse update(Long id, RoomAmenityUpdateRequest req);
    RoomAmenityResponse getById(Long id);
    PageResponse<RoomAmenityResponse> getAll(int page, int size);
    void delete(Long id);
}
