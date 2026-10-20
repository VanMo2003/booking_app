package com.vanmo.booking_app_be.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.service.ExcelReportService;
import com.vanmo.booking_app_be.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExcelReportService excelReportService;

    @PostMapping
    public ApiResponse<ReportResponse> create(@Valid @RequestBody ReportCreateRequest req) {
        return ApiResponse.ok(reportService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReportResponse> update(@PathVariable Long id, @Valid @RequestBody ReportUpdateRequest req) {
        return ApiResponse.ok(reportService.update(id, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(reportService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReportResponse>> getAll(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueByDate(hotelId, from, to));
    }

    @GetMapping("/monthly")
    public ApiResponse<List<RevenueByMonthDTO>> revenueByMonth(@RequestParam Long hotelId, @RequestParam Integer year) {
        return ApiResponse.ok(reportService.getRevenueByMonth(hotelId, year));
    }

    @GetMapping("/yearly")
    public ApiResponse<List<RevenueByYearDTO>> revenueByYear(@RequestParam Long hotelId) {
        return ApiResponse.ok(reportService.getRevenueByYear(hotelId));
    }

    @GetMapping("/monthly/last-5-months")
    public ApiResponse<List<RevenueByMonthDTO>> revenueLast5Months(@RequestParam Long hotelId) {
        return ApiResponse.ok(reportService.getRevenueLast5Months(hotelId));
    }

    // API 1: doanh thu theo tuần (range)
    @GetMapping("/weekly")
    public ApiResponse<List<RevenueByWeekDTO>> revenueByWeek(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueByWeek(hotelId, from, to));
    }

    // API 2: doanh thu từng tuần trong tháng
    @GetMapping("/weekly-in-month")
    public ApiResponse<List<RevenueByWeekDTO>> revenueByWeekInMonth(
            @RequestParam Long hotelId, @RequestParam Integer year, @RequestParam Integer month) {
        return ApiResponse.ok(reportService.getRevenueByWeekInMonth(hotelId, year, month));
    }

    @GetMapping("/chain/daily")
    public ApiResponse<List<RevenueByDateDTO>> revenueByDateForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueByDateForChain(hotelChainId, from, to));
    }

    @GetMapping("/chain/monthly")
    public ApiResponse<List<RevenueByMonthDTO>> revenueByMonthForChain(
            @RequestParam Long hotelChainId, @RequestParam Integer year) {
        return ApiResponse.ok(reportService.getRevenueByMonthForChain(hotelChainId, year));
    }

    @GetMapping("/chain/yearly")
    public ApiResponse<List<RevenueByYearDTO>> revenueByYearForChain(@RequestParam Long hotelChainId) {
        return ApiResponse.ok(reportService.getRevenueByYearForChain(hotelChainId));
    }

    @GetMapping("/chain/monthly/last-5-months")
    public ApiResponse<List<RevenueByMonthDTO>> revenueLast5MonthsForChain(@RequestParam Long hotelChainId) {
        return ApiResponse.ok(reportService.getRevenueLast5MonthsForChain(hotelChainId));
    }

    @GetMapping("/chain/weekly")
    public ApiResponse<List<RevenueByWeekDTO>> revenueByWeekForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueByWeekForChain(hotelChainId, from, to));
    }

    @GetMapping("/chain/weekly-in-month")
    public ApiResponse<List<RevenueByWeekDTO>> revenueByWeekInMonthForChain(
            @RequestParam Long hotelChainId, @RequestParam Integer year, @RequestParam Integer month) {
        return ApiResponse.ok(reportService.getRevenueByWeekInMonthForChain(hotelChainId, year, month));
    }

    // ---- detailed reports: one branch ----

    @GetMapping("/occupancy")
    public ApiResponse<OccupancyResponse> occupancy(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getOccupancy(hotelId, from, to));
    }

    @GetMapping("/occupancy/daily")
    public ApiResponse<List<OccupancyByDateDTO>> occupancyDaily(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getOccupancyByDate(hotelId, from, to));
    }

    @GetMapping("/revenue-by-room-type")
    public ApiResponse<List<RevenueByRoomTypeDTO>> revenueByRoomType(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueByRoomType(hotelId, from, to));
    }

    @GetMapping("/revenue-by-service")
    public ApiResponse<List<RevenueByServiceDTO>> revenueByService(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueByService(hotelId, from, to));
    }

    @GetMapping("/top-customers")
    public ApiResponse<List<TopCustomerDTO>> topCustomers(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(reportService.getTopCustomers(hotelId, from, to, limit));
    }

    @GetMapping("/staff-performance")
    public ApiResponse<List<StaffPerformanceDTO>> staffPerformance(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getStaffPerformance(hotelId, from, to));
    }

    @GetMapping("/cancellation-rate")
    public ApiResponse<CancellationRateResponse> cancellationRate(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getCancellationRate(hotelId, from, to));
    }

    // ---- detailed reports: whole chain ----

    @GetMapping("/chain/occupancy")
    public ApiResponse<OccupancyResponse> occupancyForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getOccupancyForChain(hotelChainId, from, to));
    }

    @GetMapping("/chain/revenue-by-room-type")
    public ApiResponse<List<RevenueByRoomTypeDTO>> revenueByRoomTypeForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueByRoomTypeForChain(hotelChainId, from, to));
    }

    @GetMapping("/chain/revenue-by-service")
    public ApiResponse<List<RevenueByServiceDTO>> revenueByServiceForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueByServiceForChain(hotelChainId, from, to));
    }

    @GetMapping("/chain/top-customers")
    public ApiResponse<List<TopCustomerDTO>> topCustomersForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(reportService.getTopCustomersForChain(hotelChainId, from, to, limit));
    }

    @GetMapping("/chain/staff-performance")
    public ApiResponse<List<StaffPerformanceDTO>> staffPerformanceForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getStaffPerformanceForChain(hotelChainId, from, to));
    }

    @GetMapping("/chain/cancellation-rate")
    public ApiResponse<CancellationRateResponse> cancellationRateForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getCancellationRateForChain(hotelChainId, from, to));
    }

    // ---- revenue summary: KPIs next to the previous period ----

    @GetMapping("/summary")
    public ApiResponse<RevenueSummaryResponse> revenueSummary(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueSummary(hotelId, from, to));
    }

    @GetMapping("/chain/summary")
    public ApiResponse<RevenueSummaryResponse> revenueSummaryForChain(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.getRevenueSummaryForChain(hotelChainId, from, to));
    }

    // ---- Excel export ----

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportHotelReport(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] workbook = excelReportService.buildHotelReport(hotelId, from, to);
        return xlsx(workbook, "bao-cao-co-so-" + hotelId + "_" + from + "_" + to + ".xlsx");
    }

    @GetMapping("/chain/export")
    public ResponseEntity<byte[]> exportChainReport(
            @RequestParam Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] workbook = excelReportService.buildChainReport(hotelChainId, from, to);
        return xlsx(workbook, "bao-cao-chuoi-" + hotelChainId + "_" + from + "_" + to + ".xlsx");
    }

    private ResponseEntity<byte[]> xlsx(byte[] body, String fileName) {
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(body);
    }
}
