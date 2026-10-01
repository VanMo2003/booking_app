package com.vanmo.booking_app_be.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.TourBookingCancelRequest;
import com.vanmo.booking_app_be.dto.request.TourBookingCreateRequest;
import com.vanmo.booking_app_be.dto.response.TourBookingResponse;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.service.TourBookingService;

import lombok.RequiredArgsConstructor;

/**
 * Tour orders. Guests place them (or the AI receptionist does in chat) and can cancel their own; the branch team
 * (owner, manager, staff) confirms, completes or cancels them. Admins can only read.
 */
@RestController
@RequestMapping("/tour-bookings")
@RequiredArgsConstructor
public class TourBookingController {

    private static final String BRANCH_TEAM = "hasAnyAuthority('ROLE_HOTEL_OWNER', 'ROLE_HOTEL_MANAGER', 'ROLE_STAFF')";

    private final TourBookingService tourBookingService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ApiResponse<TourBookingResponse> create(@Valid @RequestBody TourBookingCreateRequest req) {
        return ApiResponse.ok(tourBookingService.create(req));
    }

    @GetMapping("/me")
    public ApiResponse<List<TourBookingResponse>> getMine() {
        return ApiResponse.ok(tourBookingService.getMine());
    }

    @GetMapping
    public ApiResponse<List<TourBookingResponse>> getByHotel(
            @RequestParam Long hotelId, @RequestParam(required = false) TourBookingStatus status) {
        return ApiResponse.ok(tourBookingService.getByHotel(hotelId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<TourBookingResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(tourBookingService.getById(id));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize(BRANCH_TEAM)
    public ApiResponse<TourBookingResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(tourBookingService.confirm(id));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize(BRANCH_TEAM)
    public ApiResponse<TourBookingResponse> complete(@PathVariable Long id) {
        return ApiResponse.ok(tourBookingService.complete(id));
    }

    /** Body {@code {"reason": "..."}} - optional for the guest, required for the branch team. */
    @PutMapping("/{id}/cancel")
    public ApiResponse<TourBookingResponse> cancel(
            @PathVariable Long id, @Valid @RequestBody(required = false) TourBookingCancelRequest req) {
        return ApiResponse.ok(tourBookingService.cancel(id, req == null ? null : req.getReason()));
    }
}
