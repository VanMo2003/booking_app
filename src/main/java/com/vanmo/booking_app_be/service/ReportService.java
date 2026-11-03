package com.vanmo.booking_app_be.service;

import java.time.LocalDate;
import java.util.List;

import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.request.RevenueTargetRequest;
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

    /** The same outlook for each branch of the chain, emptiest first. */
    List<BranchOutlookDTO> getBranchOutlookForChain(Long hotelChainId, int days);

    // ---- every room of a branch over [from, to] ----

    List<RoomPerformanceDTO> getRoomPerformance(Long hotelId, LocalDate from, LocalDate to);

    // ---- Monday to Sunday over [from, to] ----

    List<WeekdayStatsDTO> getWeekdayPattern(Long hotelId, LocalDate from, LocalDate to);

    List<WeekdayStatsDTO> getWeekdayPatternForChain(Long hotelChainId, LocalDate from, LocalDate to);

    // ---- monthly revenue goals ----

    /** Sets (or, with amount 0, removes) a branch's goal for a month; owner or manager of the branch. */
    TargetProgressResponse setRevenueTarget(RevenueTargetRequest req);

    /** month "yyyy-MM". */
    TargetProgressResponse getTargetProgress(Long hotelId, String month);

    ChainTargetProgressResponse getTargetProgressForChain(Long hotelChainId, String month);

    /**
     * Checks this month's goals and notifies the owner and manager of each branch that just reached its goal or, from
     * mid-month, projects below 80% of it - each alert once per goal. Run by the morning scheduler (no caller to
     * check); returns how many alerts went out.
     */
    int sendRevenueTargetAlerts();

    /** {@link #sendRevenueTargetAlerts} on demand, for admins (e.g. to demo it without waiting for 8:00). */
    int runRevenueTargetAlertsNow();

    /**
     * Monday digest of the week before (Monday to Sunday) against the week before that: each approved chain's owner
     * gets the chain's figures, each branch manager their branch's. Weeks with no activity on either side are skipped.
     * Run by the scheduler (no caller to check); returns how many digests went out.
     */
    int sendWeeklyDigests();

    /** {@link #sendWeeklyDigests} on demand, for admins. */
    int runWeeklyDigestsNow();

    // ---- revenue trend (bookings + tours) by day / week / month / year over [from, to] ----

    List<TrendBucketDTO> getRevenueTrend(Long hotelId, LocalDate from, LocalDate to, String granularity);

    List<TrendBucketDTO> getRevenueTrendForChain(Long hotelChainId, LocalDate from, LocalDate to, String granularity);

    // ---- completed bookings whose payment was never recorded ----

    List<ReceivableDTO> getReceivables(Long hotelId);

    List<ReceivableDTO> getReceivablesForChain(Long hotelChainId);

    // ---- when orders are placed: weekday x hour ----

    BookingHoursResponse getBookingHours(Long hotelId, LocalDate from, LocalDate to);

    BookingHoursResponse getBookingHoursForChain(Long hotelChainId, LocalDate from, LocalDate to);

    // ---- money recorded as received, by day and method (at most 92 days) ----

    CollectionsResponse getCollections(Long hotelId, LocalDate from, LocalDate to);

    CollectionsResponse getCollectionsForChain(Long hotelChainId, LocalDate from, LocalDate to);

    // ---- where guests come from, and what they rate ----

    List<GuestOriginDTO> getGuestOrigins(Long hotelId, LocalDate from, LocalDate to);

    List<GuestOriginDTO> getGuestOriginsForChain(Long hotelChainId, LocalDate from, LocalDate to);

    RatingReportResponse getRatings(Long hotelId, LocalDate from, LocalDate to);

    RatingReportResponse getRatingsForChain(Long hotelChainId, LocalDate from, LocalDate to);

    // ---- what the chat assistant answered and sold over [from, to] ----

    AiAssistantReportResponse getAiAssistantReport(Long hotelId, LocalDate from, LocalDate to);

    AiAssistantReportResponse getAiAssistantReportForChain(Long hotelChainId, LocalDate from, LocalDate to);
}
