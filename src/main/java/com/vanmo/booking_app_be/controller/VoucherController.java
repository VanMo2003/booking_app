package com.vanmo.booking_app_be.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.VoucherCheckRequest;
import com.vanmo.booking_app_be.dto.request.VoucherRequest;
import com.vanmo.booking_app_be.dto.response.VoucherCheckResponse;
import com.vanmo.booking_app_be.dto.response.VoucherResponse;
import com.vanmo.booking_app_be.service.VoucherService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
public class VoucherController {
    private final VoucherService voucherService;

    @PostMapping
    public ApiResponse<VoucherResponse> create(@Valid @RequestBody VoucherRequest req) {
        return ApiResponse.ok(voucherService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<VoucherResponse> update(@PathVariable Long id, @Valid @RequestBody VoucherRequest req) {
        return ApiResponse.ok(voucherService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        voucherService.delete(id);
        return ApiResponse.ok(null);
    }

    /** The team's list: a chain owner's (hotelChainId) or a branch's (hotelId). */
    @GetMapping
    public ApiResponse<List<VoucherResponse>> list(
            @RequestParam(required = false) Long hotelChainId, @RequestParam(required = false) Long hotelId) {
        return ApiResponse.ok(voucherService.listForTeam(hotelChainId, hotelId));
    }

    /** Public: vouchers usable today at a branch, or anywhere without hotelId. */
    @GetMapping("/available")
    public ApiResponse<List<VoucherResponse>> available(@RequestParam(required = false) Long hotelId) {
        return ApiResponse.ok(voucherService.available(hotelId));
    }

    @PostMapping("/check")
    public ApiResponse<VoucherCheckResponse> check(@Valid @RequestBody VoucherCheckRequest req) {
        return ApiResponse.ok(voucherService.check(req));
    }
}
