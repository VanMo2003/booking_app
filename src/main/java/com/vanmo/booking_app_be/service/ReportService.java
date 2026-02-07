package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    ReportResponse create(ReportCreateRequest req);
    ReportResponse update(Long id, ReportUpdateRequest req);
    ReportResponse getById(Long id);
    PageResponse<ReportResponse> getAll(int page, int size);
    void delete(Long id);

    List<RevenueByDateDTO> getRevenueByDate(Long hotelId, LocalDate from, LocalDate to);
    List<RevenueByMonthDTO> getRevenueByMonth(Long hotelId, Integer year);
    List<RevenueByYearDTO> getRevenueByYear(Long hotelId);
    List<RevenueByMonthDTO> getRevenueLast5Months(Long hotelId);
    List<RevenueByWeekDTO> getRevenueByWeek(Long hotelId, LocalDate from, LocalDate to);
    List<RevenueByWeekDTO> getRevenueByWeekInMonth(Long hotelId, Integer year, Integer month);
}
