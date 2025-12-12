package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.RoomAmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomAmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomAmenityResponse;
import com.vanmo.booking_app_be.service.RoomAmenityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roomAmenitys")
@RequiredArgsConstructor
public class RoomAmenityController {

    private final RoomAmenityService roomAmenityService;

    @PostMapping
    public ApiResponse<RoomAmenityResponse> create(@Valid @RequestBody RoomAmenityCreateRequest req) {
        return ApiResponse.ok(roomAmenityService.create(req), "Create roomAmenity success");
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomAmenityResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody RoomAmenityUpdateRequest req) {
        return ApiResponse.ok(roomAmenityService.update(id, req), "Update roomAmenity success");
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomAmenityResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomAmenityService.getById(id), "Get roomAmenity success");
    }

    @GetMapping
    public ApiResponse<PageResponse<RoomAmenityResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(roomAmenityService.getAll(page, size), "Get roomAmenitys success");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomAmenityService.delete(id);
        return ApiResponse.ok(null, "Delete roomAmenity success");
    }
}
