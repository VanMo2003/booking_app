package com.vanmo.booking_app_be.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.TourCreateRequest;
import com.vanmo.booking_app_be.dto.request.TourUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.TourDestinationDTO;
import com.vanmo.booking_app_be.dto.response.TourHomeResponse;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.dto.response.TourRoomAvailabilityResponse;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;
import com.vanmo.booking_app_be.service.TourBookingService;
import com.vanmo.booking_app_be.service.TourService;

import lombok.RequiredArgsConstructor;

/** Tours of a branch. Reads are public; writes are for the branch's owner or manager (checked in the service). */
@RestController
@RequestMapping("/tours")
@RequiredArgsConstructor
public class TourController {

    private static final String TOUR_EDITORS = "hasAnyAuthority('ROLE_HOTEL_OWNER', 'ROLE_HOTEL_MANAGER')";

    private final TourService tourService;
    private final TourBookingService tourBookingService;

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

    /** Public: tours across every open branch, by text (name, destination, address), theme and price. */
    @GetMapping("/search")
    public ApiResponse<PageResponse<TourResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TravelTheme theme,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(tourService.search(q, theme, maxPrice, page, size));
    }

    @GetMapping("/destinations")
    public ApiResponse<List<TourDestinationDTO>> destinations() {
        return ApiResponse.ok(tourService.destinations());
    }

    /** Public: the guests' home screen; {@code interests} = the themes the guest picked, comma separated. */
    @GetMapping("/home")
    public ApiResponse<TourHomeResponse> home(@RequestParam(required = false) Set<TravelTheme> interests) {
        return ApiResponse.ok(tourService.home(interests == null ? Set.of() : interests));
    }

    @GetMapping("/{id}")
    public ApiResponse<TourResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(tourService.getById(id));
    }

    /** Public: the stay sold with the tour from {@code date} and which of the package's rooms are still free. */
    @GetMapping("/{id}/rooms/available")
    public ApiResponse<TourRoomAvailabilityResponse> availableRooms(
            @PathVariable Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(tourBookingService.availableRooms(id, date));
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
