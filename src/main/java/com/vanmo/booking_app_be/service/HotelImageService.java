package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.HotelImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelImageResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface HotelImageService {
    HotelImageResponse create(HotelImageCreateRequest req);

    HotelImageResponse update(Long id, HotelImageUpdateRequest req);

    HotelImageResponse getById(Long id);

    PageResponse<HotelImageResponse> getAll(int page, int size);

    void delete(Long id);
}
