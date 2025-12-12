package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.RoomTypeCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomTypeUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomTypeResponse;
import com.vanmo.booking_app_be.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roomTypes")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @PostMapping
    public ApiResponse<RoomTypeResponse> create(@Valid @RequestBody RoomTypeCreateRequest req) {
        return ApiResponse.ok(roomTypeService.create(req), "Create roomType success");
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomTypeResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody RoomTypeUpdateRequest req) {
        return ApiResponse.ok(roomTypeService.update(id, req), "Update roomType success");
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomTypeResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomTypeService.getById(id), "Get roomType success");
    }

    @GetMapping
    public ApiResponse<PageResponse<RoomTypeResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(roomTypeService.getAll(page, size), "Get roomTypes success");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ApiResponse.ok(null, "Delete roomType success");
    }
}
