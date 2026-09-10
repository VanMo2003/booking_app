package com.vanmo.booking_app_be.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.AmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.AmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.AmenityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    @PostMapping
    public ApiResponse<AmenityResponse> create(@Valid @RequestBody AmenityCreateRequest req) {
        return ApiResponse.ok(amenityService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<AmenityResponse> update(@PathVariable Long id, @Valid @RequestBody AmenityUpdateRequest req) {
        return ApiResponse.ok(amenityService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<AmenityResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(amenityService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<AmenityResponse>> getAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(amenityService.getAll(page, size));
    }

    @GetMapping("/hotel/{hotelId}")
    public ApiResponse<List<AmenityResponse>> getAllByHotel(@PathVariable Long hotelId) {
        return ApiResponse.ok(amenityService.getAllByHotel(hotelId));
    }

    @GetMapping("/room")
    public ApiResponse<List<AmenityResponse>> getAllByRoom(
            @RequestParam(defaultValue = "0") Long hotelId, @RequestParam(defaultValue = "0") Long roomId) {
        return ApiResponse.ok(amenityService.getAllByRoom(hotelId, roomId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("!hasAuthority('ROLE_STAFF')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        amenityService.delete(id);
        return ApiResponse.ok(null);
    }
}
