package com.vanmo.booking_app_be.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.TourCreateRequest;
import com.vanmo.booking_app_be.dto.request.TourUpdateRequest;
import com.vanmo.booking_app_be.dto.response.TourResponse;

public interface TourService {
    TourResponse create(TourCreateRequest req);

    TourResponse update(Long id, TourUpdateRequest req);

    TourResponse uploadImage(Long id, MultipartFile file);

    TourResponse getById(Long id);

    /** Running tours first, cheapest first. */
    List<TourResponse> getAllByHotel(Long hotelId);

    void delete(Long id);
}
