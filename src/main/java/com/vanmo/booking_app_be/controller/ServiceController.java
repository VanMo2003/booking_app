package com.vanmo.booking_app_be.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.ServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.ServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ServiceResponse;
import com.vanmo.booking_app_be.service.ServiceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @PostMapping
    public ApiResponse<ServiceResponse> create(@Valid @RequestBody ServiceCreateRequest req) {
        return ApiResponse.ok(serviceService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceUpdateRequest req) {
        return ApiResponse.ok(serviceService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(serviceService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<ServiceResponse>> getAllByHotel(@RequestParam(name = "hotelId") Long hotelId) {
        return ApiResponse.ok(serviceService.getAllByHotel(hotelId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("!hasAuthority('ROLE_STAFF')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ApiResponse.ok(null);
    }
}
