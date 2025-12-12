package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.PositionCreateRequest;
import com.vanmo.booking_app_be.dto.request.PositionUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.PositionResponse;
import com.vanmo.booking_app_be.service.PositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @PostMapping
    public ApiResponse<PositionResponse> create(@Valid @RequestBody PositionCreateRequest req) {
        return ApiResponse.ok(positionService.create(req), "Create position success");
    }

    @PutMapping("/{id}")
    public ApiResponse<PositionResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody PositionUpdateRequest req) {
        return ApiResponse.ok(positionService.update(id, req), "Update position success");
    }

    @GetMapping("/{id}")
    public ApiResponse<PositionResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(positionService.getById(id), "Get position success");
    }

    @GetMapping
    public ApiResponse<PageResponse<PositionResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(positionService.getAll(page, size), "Get positions success");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return ApiResponse.ok(null, "Delete position success");
    }
}
