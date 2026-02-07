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

import java.util.List;

@RestController
@RequestMapping("/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @PostMapping
    public ApiResponse<PositionResponse> create(@Valid @RequestBody PositionCreateRequest req) {
        return ApiResponse.ok(positionService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<PositionResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody PositionUpdateRequest req) {
        return ApiResponse.ok(positionService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<PositionResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(positionService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<PositionResponse>> getAll() {
        return ApiResponse.ok(positionService.getAll());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return ApiResponse.ok(null);
    }
}
