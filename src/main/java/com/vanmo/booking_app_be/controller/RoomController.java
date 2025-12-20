package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.RoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomResponse;
import com.vanmo.booking_app_be.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ApiResponse<RoomResponse> create(@Valid @RequestBody RoomCreateRequest req) {
        return ApiResponse.ok(roomService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RoomUpdateRequest req
    ) {
        return ApiResponse.ok(roomService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RoomResponse>> getAllByHotel(
            @RequestParam(name = "hotelId") Long hotelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(roomService.getAllByHotel(hotelId, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ApiResponse.ok(null);
    }
}
