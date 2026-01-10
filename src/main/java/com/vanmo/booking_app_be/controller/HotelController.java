package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelDetailResponse;
import com.vanmo.booking_app_be.dto.response.ListHotelResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @PostMapping
    public ApiResponse<ListHotelResponse> create(@Valid @RequestBody HotelCreateRequest req) {
        return ApiResponse.ok(hotelService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ListHotelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody HotelUpdateRequest req
    ) {
        return ApiResponse.ok(hotelService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<HotelDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(hotelService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ListHotelResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(hotelService.getAll(page, size));
    }

    @GetMapping("/category")
    public ApiResponse<List<ListHotelResponse>> getAllByCategory(
            @RequestParam(name = "name") String name
    ) {
        return ApiResponse.ok(hotelService.getAllByCategory(name));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        hotelService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/images")
    public ApiResponse<List<String>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ApiResponse.ok(hotelService.uploadImages(id, files));
    }
}
