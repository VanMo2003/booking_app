package com.vanmo.booking_app_be.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.HotelImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelImageResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.HotelImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hotelImages")
@RequiredArgsConstructor
public class HotelImageController {

    private final HotelImageService hotelImageService;

    @PostMapping
    public ApiResponse<HotelImageResponse> create(@Valid @RequestBody HotelImageCreateRequest req) {
        return ApiResponse.ok(hotelImageService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<HotelImageResponse> update(
            @PathVariable Long id, @Valid @RequestBody HotelImageUpdateRequest req) {
        return ApiResponse.ok(hotelImageService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<HotelImageResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(hotelImageService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<HotelImageResponse>> getAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(hotelImageService.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hotelImageService.delete(id);
        return ApiResponse.ok(null);
    }
}
