package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.AmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.AmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

import java.util.List;

public interface AmenityService {
    AmenityResponse create(AmenityCreateRequest req);
    AmenityResponse update(Long id, AmenityUpdateRequest req);
    AmenityResponse getById(Long id);
    PageResponse<AmenityResponse> getAll(int page, int size);
    List<AmenityResponse> getAllByHotel(Long hotelId);
    List<AmenityResponse> getAllByRoom(Long hotelId, Long roomId);
    void delete(Long id);
}
