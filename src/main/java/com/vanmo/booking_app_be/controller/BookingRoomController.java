package com.vanmo.booking_app_be.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.BookingRoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingRoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingRoomResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.service.BookingRoomService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookingRooms")
@RequiredArgsConstructor
public class BookingRoomController {

    private final BookingRoomService bookingRoomService;

    @PostMapping
    public ApiResponse<BookingRoomResponse> create(@Valid @RequestBody BookingRoomCreateRequest req) {
        return ApiResponse.ok(bookingRoomService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<BookingRoomResponse> update(
            @PathVariable Long id, @Valid @RequestBody BookingRoomUpdateRequest req) {
        return ApiResponse.ok(bookingRoomService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingRoomResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(bookingRoomService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<BookingRoomResponse>> getAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(bookingRoomService.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bookingRoomService.delete(id);
        return ApiResponse.ok(null);
    }
}
