package com.vanmo.booking_app_be.service;

import java.time.LocalDate;
import java.util.List;

import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;

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

    List<RevenueByDateDTO> getRevenueByDateForChain(Long hotelChainId, LocalDate from, LocalDate to);

    List<RevenueByMonthDTO> getRevenueByMonthForChain(Long hotelChainId, Integer year);

    List<RevenueByYearDTO> getRevenueByYearForChain(Long hotelChainId);

    List<RevenueByMonthDTO> getRevenueLast5MonthsForChain(Long hotelChainId);

    List<RevenueByWeekDTO> getRevenueByWeekForChain(Long hotelChainId, LocalDate from, LocalDate to);

    List<RevenueByWeekDTO> getRevenueByWeekInMonthForChain(Long hotelChainId, Integer year, Integer month);

    // ---- detailed reports: branch level ----

    OccupancyResponse getOccupancy(Long hotelId, LocalDate from, LocalDate to);

    List<OccupancyByDateDTO> getOccupancyByDate(Long hotelId, LocalDate from, LocalDate to);

    List<RevenueByRoomTypeDTO> getRevenueByRoomType(Long hotelId, LocalDate from, LocalDate to);

    List<RevenueByServiceDTO> getRevenueByService(Long hotelId, LocalDate from, LocalDate to);

    List<TopCustomerDTO> getTopCustomers(Long hotelId, LocalDate from, LocalDate to, int limit);

    List<StaffPerformanceDTO> getStaffPerformance(Long hotelId, LocalDate from, LocalDate to);

    CancellationRateResponse getCancellationRate(Long hotelId, LocalDate from, LocalDate to);

    // ---- detailed reports: chain level ----

    OccupancyResponse getOccupancyForChain(Long hotelChainId, LocalDate from, LocalDate to);

    List<RevenueByRoomTypeDTO> getRevenueByRoomTypeForChain(Long hotelChainId, LocalDate from, LocalDate to);

    List<RevenueByServiceDTO> getRevenueByServiceForChain(Long hotelChainId, LocalDate from, LocalDate to);

    List<TopCustomerDTO> getTopCustomersForChain(Long hotelChainId, LocalDate from, LocalDate to, int limit);

    List<StaffPerformanceDTO> getStaffPerformanceForChain(Long hotelChainId, LocalDate from, LocalDate to);

    CancellationRateResponse getCancellationRateForChain(Long hotelChainId, LocalDate from, LocalDate to);

    // ---- revenue summary: the period's KPIs next to the period just before it ----

    RevenueSummaryResponse getRevenueSummary(Long hotelId, LocalDate from, LocalDate to);

    RevenueSummaryResponse getRevenueSummaryForChain(Long hotelChainId, LocalDate from, LocalDate to);

    // ---- completed tour orders per tour (tour date in [from, to]) ----

    List<RevenueByTourDTO> getRevenueByTour(Long hotelId, LocalDate from, LocalDate to);

    List<RevenueByTourDTO> getRevenueByTourForChain(Long hotelChainId, LocalDate from, LocalDate to);

    // ---- the chain's branches side by side over [from, to] ----

    List<BranchPerformanceDTO> getBranchPerformanceForChain(Long hotelChainId, LocalDate from, LocalDate to);

    // ---- revenue still to come over the next `days` days, money still to collect ----

    RevenueOutlookResponse getRevenueOutlook(Long hotelId, int days);

    RevenueOutlookResponse getRevenueOutlookForChain(Long hotelChainId, int days);

    // ---- every room of a branch over [from, to] ----

    List<RoomPerformanceDTO> getRoomPerformance(Long hotelId, LocalDate from, LocalDate to);
}
