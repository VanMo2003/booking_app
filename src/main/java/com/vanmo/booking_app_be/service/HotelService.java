package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelDetailResponse;
import com.vanmo.booking_app_be.dto.response.ListHotelResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface HotelService {
    ListHotelResponse create(HotelCreateRequest req);
    ListHotelResponse update(Long id, HotelUpdateRequest req);
    HotelDetailResponse getById(Long id);
    PageResponse<ListHotelResponse> getAll(int page, int size);
    List<ListHotelResponse> getAllByCategory(String name);
    List<String> uploadImages(Long hotelId, List<MultipartFile> files);
    void delete(Long id);
    List<ListHotelResponse> searchByDate(LocalDate checkinDate,LocalDate checkoutDate);
}
