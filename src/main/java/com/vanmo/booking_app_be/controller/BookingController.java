package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ListBookingResponse;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ApiResponse<BookingResponse> create(@Valid @RequestBody BookingCreateRequest req) {
        return ApiResponse.ok(bookingService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<BookingResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody BookingUpdateRequest req) {
        return ApiResponse.ok(bookingService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<ListBookingResponse>> getAllByHotelOrUser(
            @RequestParam(defaultValue = "0") Long hotelId,
            @RequestParam(defaultValue = "0") Long customerId,
            @RequestParam(defaultValue = "") String bookingStatus) {
        if (hotelId != 0) {
            return ApiResponse.ok(bookingService.getAllByHotel(hotelId, bookingStatus));
        }else{
            return ApiResponse.ok(bookingService.getAllByCustomer(customerId, bookingStatus));
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<BookingResponse> confirmBooking(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.confirmBooking(id));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.cancelBooking(id));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<BookingResponse> completeBooking(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.completeBooking(id));
    }

}
