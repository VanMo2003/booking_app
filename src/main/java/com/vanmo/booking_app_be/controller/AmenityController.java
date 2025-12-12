package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.AmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.AmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.AmenityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    @PostMapping
    public ApiResponse<AmenityResponse> create(@Valid @RequestBody AmenityCreateRequest req) {
        return ApiResponse.ok(amenityService.create(req), "Create amenity success");
    }

    @PutMapping("/{id}")
    public ApiResponse<AmenityResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody AmenityUpdateRequest req) {
        return ApiResponse.ok(amenityService.update(id, req), "Update amenity success");
    }

    @GetMapping("/{id}")
    public ApiResponse<AmenityResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(amenityService.getById(id), "Get amenity success");
    }

    @GetMapping
    public ApiResponse<PageResponse<AmenityResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(amenityService.getAll(page, size), "Get amenities success");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        amenityService.delete(id);
        return ApiResponse.ok(null, "Delete amenity success");
    }
}
