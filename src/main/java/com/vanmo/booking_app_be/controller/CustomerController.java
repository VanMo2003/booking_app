package com.vanmo.booking_app_be.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.CustomerCreateRequest;
import com.vanmo.booking_app_be.dto.request.CustomerLinkByPhoneRequest;
import com.vanmo.booking_app_be.dto.request.CustomerUpdateRequest;
import com.vanmo.booking_app_be.dto.response.CustomerResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.CustomerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/by-hotel")
    public ApiResponse<List<CustomerResponse>> getAllByHotel(@RequestParam Long hotelId) {
        return ApiResponse.ok(customerService.getAllByHotel(hotelId));
    }

    /** Front-desk lookup so a returning walk-in guest doesn't get a duplicate profile. */
    @GetMapping("/search")
    public ApiResponse<List<CustomerResponse>> searchByPhone(@RequestParam String phoneNumber) {
        return ApiResponse.ok(customerService.searchByPhone(phoneNumber));
    }

    /** A newly registered customer claims the walk-in profile staff created for them earlier. */
    @PostMapping("/link-by-phone")
    public ApiResponse<CustomerResponse> linkByPhone(@Valid @RequestBody CustomerLinkByPhoneRequest req) {
        return ApiResponse.ok(customerService.linkByPhone(req.getPhoneNumber()));
    }

    @PostMapping
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest req) {
        return ApiResponse.ok(customerService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest req) {
        return ApiResponse.ok(customerService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> getAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(customerService.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.ok(null);
    }
}
