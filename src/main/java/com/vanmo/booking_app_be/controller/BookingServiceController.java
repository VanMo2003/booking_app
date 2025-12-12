package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.BookingServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.BookingServiceResponse;
import com.vanmo.booking_app_be.service.BookingServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookingServices")
@RequiredArgsConstructor
public class BookingServiceController {

    private final BookingServiceService bookingServiceService;

    @PostMapping
    public ApiResponse<BookingServiceResponse> create(@Valid @RequestBody BookingServiceCreateRequest req) {
        return ApiResponse.ok(bookingServiceService.create(req), "Create bookingService success");
    }

    @PutMapping("/{id}")
    public ApiResponse<BookingServiceResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody BookingServiceUpdateRequest req) {
        return ApiResponse.ok(bookingServiceService.update(id, req), "Update bookingService success");
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingServiceResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(bookingServiceService.getById(id), "Get bookingService success");
    }

    @GetMapping
    public ApiResponse<PageResponse<BookingServiceResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(bookingServiceService.getAll(page, size), "Get bookingServices success");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bookingServiceService.delete(id);
        return ApiResponse.ok(null, "Delete bookingService success");
    }
}
