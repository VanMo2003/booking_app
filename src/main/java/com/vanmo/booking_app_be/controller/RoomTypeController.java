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

import java.util.List;

@RestController
@RequestMapping("/roomTypes")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @PostMapping
    public ApiResponse<RoomTypeResponse> create(@Valid @RequestBody RoomTypeCreateRequest req) {
        return ApiResponse.ok(roomTypeService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomTypeResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody RoomTypeUpdateRequest req) {
        return ApiResponse.ok(roomTypeService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomTypeResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomTypeService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<RoomTypeResponse>> getAll() {
        return ApiResponse.ok(roomTypeService.getAll());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ApiResponse.ok(null);
    }
}
