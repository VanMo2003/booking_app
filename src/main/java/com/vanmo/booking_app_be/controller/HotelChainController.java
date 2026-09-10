package com.vanmo.booking_app_be.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.HotelChainCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelChainUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelChainDetailResponse;
import com.vanmo.booking_app_be.dto.response.ListHotelChainResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.HotelChainService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/hotel-chains")
@RequiredArgsConstructor
public class HotelChainController {

    private final HotelChainService hotelChainService;

    @PostMapping
    public ApiResponse<ListHotelChainResponse> create(@Valid @RequestBody HotelChainCreateRequest req) {
        return ApiResponse.ok(hotelChainService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ListHotelChainResponse> update(
            @PathVariable Long id, @Valid @RequestBody HotelChainUpdateRequest req) {
        return ApiResponse.ok(hotelChainService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<HotelChainDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(hotelChainService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ListHotelChainResponse>> getAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(hotelChainService.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hotelChainService.delete(id);
        return ApiResponse.ok(null);
    }
}
