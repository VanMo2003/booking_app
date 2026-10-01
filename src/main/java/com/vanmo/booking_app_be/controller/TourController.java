package com.vanmo.booking_app_be.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.TourCreateRequest;
import com.vanmo.booking_app_be.dto.request.TourUpdateRequest;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.service.TourService;

import lombok.RequiredArgsConstructor;

/** Tours of a branch. Reads are public; writes are for the branch's owner or manager (checked in the service). */
@RestController
@RequestMapping("/tours")
@RequiredArgsConstructor
public class TourController {

    private static final String TOUR_EDITORS = "hasAnyAuthority('ROLE_HOTEL_OWNER', 'ROLE_HOTEL_MANAGER')";

    private final TourService tourService;

    @PostMapping
    @PreAuthorize(TOUR_EDITORS)
    public ApiResponse<TourResponse> create(@Valid @RequestBody TourCreateRequest req) {
        return ApiResponse.ok(tourService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize(TOUR_EDITORS)
    public ApiResponse<TourResponse> update(@PathVariable Long id, @Valid @RequestBody TourUpdateRequest req) {
        return ApiResponse.ok(tourService.update(id, req));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(TOUR_EDITORS)
    public ApiResponse<TourResponse> uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(tourService.uploadImage(id, file));
    }

    @GetMapping("/{id}")
    public ApiResponse<TourResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(tourService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<TourResponse>> getAllByHotel(@RequestParam(name = "hotelId") Long hotelId) {
        return ApiResponse.ok(tourService.getAllByHotel(hotelId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(TOUR_EDITORS)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tourService.delete(id);
        return ApiResponse.ok(null);
    }
}
