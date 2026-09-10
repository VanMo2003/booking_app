package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.HotelChainCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelChainUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelChainDetailResponse;
import com.vanmo.booking_app_be.dto.response.ListHotelChainResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface HotelChainService {
    ListHotelChainResponse create(HotelChainCreateRequest req);

    ListHotelChainResponse update(Long id, HotelChainUpdateRequest req);

    HotelChainDetailResponse getById(Long id);

    PageResponse<ListHotelChainResponse> getAll(int page, int size);

    void delete(Long id);
}
