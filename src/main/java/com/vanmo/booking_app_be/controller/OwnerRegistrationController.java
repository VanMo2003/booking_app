package com.vanmo.booking_app_be.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.OwnerRegistrationRequest;
import com.vanmo.booking_app_be.dto.request.OwnerRejectRequest;
import com.vanmo.booking_app_be.dto.request.OwnerResubmitRequest;
import com.vanmo.booking_app_be.dto.response.ListHotelChainResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.enums.ApprovalStatus;
import com.vanmo.booking_app_be.service.OwnerRegistrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner-registrations")
@RequiredArgsConstructor
public class OwnerRegistrationController {

    private final OwnerRegistrationService ownerRegistrationService;

    /** Public sign-up: creates the HOTEL_OWNER account and its hotel chain, waiting for an admin's review. */
    @PostMapping
    public ApiResponse<ListHotelChainResponse> register(@Valid @RequestBody OwnerRegistrationRequest req) {
        return ApiResponse.ok(ownerRegistrationService.register(req));
    }

    @GetMapping("/me")
    public ApiResponse<ListHotelChainResponse> getMine() {
        return ApiResponse.ok(ownerRegistrationService.getMine());
    }

    @PutMapping("/me")
    public ApiResponse<ListHotelChainResponse> resubmit(@Valid @RequestBody OwnerResubmitRequest req) {
        return ApiResponse.ok(ownerRegistrationService.resubmit(req));
    }

    @GetMapping
    public ApiResponse<PageResponse<ListHotelChainResponse>> getAll(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(ownerRegistrationService.getAll(status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ListHotelChainResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(ownerRegistrationService.getById(id));
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<ListHotelChainResponse> approve(@PathVariable Long id) {
        return ApiResponse.ok(ownerRegistrationService.approve(id));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<ListHotelChainResponse> reject(
            @PathVariable Long id, @Valid @RequestBody OwnerRejectRequest req) {
        return ApiResponse.ok(ownerRegistrationService.reject(id, req));
    }
}
