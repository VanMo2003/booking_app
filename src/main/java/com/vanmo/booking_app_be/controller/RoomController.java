package com.vanmo.booking_app_be.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.RoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.RoomDetailResponse;
import com.vanmo.booking_app_be.dto.response.RoomResponse;
import com.vanmo.booking_app_be.service.RoomService;

import lombok.RequiredArgsConstructor;

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
    public ApiResponse<RoomResponse> update(@PathVariable Long id, @Valid @RequestBody RoomUpdateRequest req) {
        return ApiResponse.ok(roomService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<RoomResponse>> getAllByHotel(
            @RequestParam(name = "hotelId") Long hotelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(roomService.getAllByHotel(hotelId, page, size));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("!hasAuthority('ROLE_STAFF')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<String>> uploadImages(
            @PathVariable Long id, @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.ok(roomService.uploadImages(id, files));
    }

    @GetMapping("/available")
    public ApiResponse<List<RoomResponse>> getRoomsByDate(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkinDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkoutDate) {
        return ApiResponse.ok(roomService.getRoomsByDate(hotelId, checkinDate, checkoutDate));
    }
}
