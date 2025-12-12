package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.ServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.ServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.ServiceResponse;
import com.vanmo.booking_app_be.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @PostMapping
    public ApiResponse<ServiceResponse> create(@Valid @RequestBody ServiceCreateRequest req) {
        return ApiResponse.ok(serviceService.create(req), "Create service success");
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody ServiceUpdateRequest req) {
        return ApiResponse.ok(serviceService.update(id, req), "Update service success");
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(serviceService.getById(id), "Get service success");
    }

    @GetMapping
    public ApiResponse<PageResponse<ServiceResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(serviceService.getAll(page, size), "Get services success");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ApiResponse.ok(null, "Delete service success");
    }
}
