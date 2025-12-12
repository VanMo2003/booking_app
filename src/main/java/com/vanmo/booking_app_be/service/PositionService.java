package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.PositionCreateRequest;
import com.vanmo.booking_app_be.dto.request.PositionUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PositionResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface PositionService {
    PositionResponse create(PositionCreateRequest req);
    PositionResponse update(Long id, PositionUpdateRequest req);
    PositionResponse getById(Long id);
    PageResponse<PositionResponse> getAll(int page, int size);
    void delete(Long id);
}
