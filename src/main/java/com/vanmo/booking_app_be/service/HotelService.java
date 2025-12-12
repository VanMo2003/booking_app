package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface HotelService {
    HotelResponse create(HotelCreateRequest req);
    HotelResponse update(Long id, HotelUpdateRequest req);
    HotelResponse getById(Long id);
    PageResponse<HotelResponse> getAll(int page, int size);
    void delete(Long id);
}
