package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.RoomImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomImageResponse;

public interface RoomImageService {
    RoomImageResponse create(RoomImageCreateRequest req);

    RoomImageResponse update(Long id, RoomImageUpdateRequest req);

    RoomImageResponse getById(Long id);

    PageResponse<RoomImageResponse> getAll(int page, int size);

    void delete(Long id);
}
