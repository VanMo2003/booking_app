package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ApiResponse<ReportResponse> create(@Valid @RequestBody ReportCreateRequest req) {
        return ApiResponse.ok(reportService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReportResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody ReportUpdateRequest req) {
        return ApiResponse.ok(reportService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(reportService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReportResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reportService.getAll(page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/daily")
    public ApiResponse<List<RevenueByDateDTO>> revenueByDate(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.ok(reportService.getRevenueByDate(hotelId, from, to));
    }

    @GetMapping("/monthly")
    public ApiResponse<List<RevenueByMonthDTO>> revenueByMonth(
            @RequestParam Long hotelId,
            @RequestParam Integer year
    ) {
        return ApiResponse.ok(reportService.getRevenueByMonth(hotelId, year));
    }

    @GetMapping("/yearly")
    public ApiResponse<List<RevenueByYearDTO>> revenueByYear(
            @RequestParam Long hotelId
    ) {
        return ApiResponse.ok(reportService.getRevenueByYear(hotelId));
    }

    @GetMapping("/monthly/last-5-months")
    public ApiResponse<List<RevenueByMonthDTO>> revenueLast5Months(
            @RequestParam Long hotelId
    ) {
        return ApiResponse.ok(reportService.getRevenueLast5Months(hotelId));
    }

    // API 1: doanh thu theo tuần (range)
    @GetMapping("/weekly")
    public ApiResponse<List<RevenueByWeekDTO>> revenueByWeek(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.ok(reportService.getRevenueByWeek(hotelId, from, to));
    }

    // API 2: doanh thu từng tuần trong tháng
    @GetMapping("/weekly-in-month")
    public ApiResponse<List<RevenueByWeekDTO>> revenueByWeekInMonth(
            @RequestParam Long hotelId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return ApiResponse.ok(reportService.getRevenueByWeekInMonth(hotelId, year, month));
    }
}
