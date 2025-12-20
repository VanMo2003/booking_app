package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.RoomImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomImageResponse;
import com.vanmo.booking_app_be.service.RoomImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roomImages")
@RequiredArgsConstructor
public class RoomImageController {

    private final RoomImageService roomImageService;

    @PostMapping
    public ApiResponse<RoomImageResponse> create(@Valid @RequestBody RoomImageCreateRequest req) {
        return ApiResponse.ok(roomImageService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomImageResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody RoomImageUpdateRequest req) {
        return ApiResponse.ok(roomImageService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomImageResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomImageService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RoomImageResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(roomImageService.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomImageService.delete(id);
        return ApiResponse.ok(null);
    }
}
