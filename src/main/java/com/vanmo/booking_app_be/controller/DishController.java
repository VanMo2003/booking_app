package com.vanmo.booking_app_be.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.DishCreateRequest;
import com.vanmo.booking_app_be.dto.request.DishUpdateRequest;
import com.vanmo.booking_app_be.dto.response.DishResponse;
import com.vanmo.booking_app_be.service.DishService;

import lombok.RequiredArgsConstructor;

/** Food menu of a branch. Reads are public; writes are for the branch's owner or manager (checked in the service). */
@RestController
@RequestMapping("/dishes")
@RequiredArgsConstructor
public class DishController {

    private static final String MENU_EDITORS = "hasAnyAuthority('ROLE_HOTEL_OWNER', 'ROLE_HOTEL_MANAGER')";

    private final DishService dishService;

    @PostMapping
    @PreAuthorize(MENU_EDITORS)
    public ApiResponse<DishResponse> create(@Valid @RequestBody DishCreateRequest req) {
        return ApiResponse.ok(dishService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize(MENU_EDITORS)
    public ApiResponse<DishResponse> update(@PathVariable Long id, @Valid @RequestBody DishUpdateRequest req) {
        return ApiResponse.ok(dishService.update(id, req));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(MENU_EDITORS)
    public ApiResponse<DishResponse> uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(dishService.uploadImage(id, file));
    }

    @GetMapping("/{id}")
    public ApiResponse<DishResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(dishService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<DishResponse>> getAllByHotel(@RequestParam(name = "hotelId") Long hotelId) {
        return ApiResponse.ok(dishService.getAllByHotel(hotelId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(MENU_EDITORS)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return ApiResponse.ok(null);
    }
}
