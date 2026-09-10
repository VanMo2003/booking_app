package com.vanmo.booking_app_be.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelDetailResponse;
import com.vanmo.booking_app_be.dto.response.ListHotelResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;

public interface HotelService {
    ListHotelResponse create(HotelCreateRequest req, List<MultipartFile> files);

    ListHotelResponse update(Long id, HotelUpdateRequest req);

    HotelDetailResponse getById(Long id, LocalDate checkinDate, LocalDate checkoutDate);

    default HotelDetailResponse getById(Long id) {
        return getById(id, null, null);
    }

    PageResponse<ListHotelResponse> getAll(int page, int size);

    List<ListHotelResponse> getAllByCategory(String name);

    List<String> uploadImages(Long hotelId, List<MultipartFile> files);

    void delete(Long id);

    List<ListHotelResponse> searchByDate(LocalDate checkinDate, LocalDate checkoutDate);
}
