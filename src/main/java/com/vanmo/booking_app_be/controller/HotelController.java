package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @PostMapping
    public ApiResponse<HotelResponse> create(@Valid @RequestBody HotelCreateRequest req) {
        return ApiResponse.ok(hotelService.create(req), "Create hotel success");
    }

    @PutMapping("/{id}")
    public ApiResponse<HotelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody HotelUpdateRequest req
    ) {
        return ApiResponse.ok(hotelService.update(id, req), "Update hotel success");
    }

    @GetMapping("/{id}")
    public ApiResponse<HotelResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(hotelService.getById(id), "Get hotel success");
    }

    @GetMapping
    public ApiResponse<PageResponse<HotelResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(hotelService.getAll(page, size), "Get hotels success");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hotelService.delete(id);
        return ApiResponse.ok(null, "Delete hotel success");
    }
}
