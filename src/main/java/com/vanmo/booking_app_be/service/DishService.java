package com.vanmo.booking_app_be.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.request.DishCreateRequest;
import com.vanmo.booking_app_be.dto.request.DishUpdateRequest;
import com.vanmo.booking_app_be.dto.response.DishResponse;

public interface DishService {
    DishResponse create(DishCreateRequest req);

    DishResponse update(Long id, DishUpdateRequest req);

    /** Replaces the dish photo with an uploaded file. */
    DishResponse uploadImage(Long id, MultipartFile file);

    DishResponse getById(Long id);

    /** The branch's whole menu, section by section (see {@link com.vanmo.booking_app_be.entity.enums.DishCategory}). */
    List<DishResponse> getAllByHotel(Long hotelId);

    void delete(Long id);
}
