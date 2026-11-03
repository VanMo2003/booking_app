package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.request.RevenueTargetRequest;
import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.Report;
import com.vanmo.booking_app_be.entity.RevenueTarget;
import com.vanmo.booking_app_be.entity.enums.ApprovalStatus;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.entity.enums.PayrollStatus;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.ReportMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.BookingRoomRepository;
import com.vanmo.booking_app_be.repository.BookingServiceRepository;
import com.vanmo.booking_app_be.repository.ChatMessageRepository;
import com.vanmo.booking_app_be.repository.ConversationRepository;
import com.vanmo.booking_app_be.repository.EmployeeRepository;
import com.vanmo.booking_app_be.repository.FeedbackRepository;
import com.vanmo.booking_app_be.repository.HotelChainRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.PayrollRepository;
import com.vanmo.booking_app_be.repository.ReportRepository;
import com.vanmo.booking_app_be.repository.RevenueTargetRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.TourBookingRepository;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.service.ReportService;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int MAX_DAILY_RANGE = 92;
    private static final int MAX_OUTLOOK_DAYS = 90;
    /** Day buckets above this many days would make an unreadable chart. */
    private static final int MAX_TREND_DAYS = 400;

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final RoomRepository roomRepository;
    private final EmployeeRepository employeeRepository;
    private final HotelRepository hotelRepository;
    private final TourBookingRepository tourBookingRepository;
    private final TourRepository tourRepository;
    private final PayrollRepository payrollRepository;
    private final RevenueTargetRepository revenueTargetRepository;
    private final NotificationService notificationService;
    private final AccountRepository accountRepository;
    private final HotelChainRepository hotelChainRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final FeedbackRepository feedbackRepository;
    private final AccessGuard accessGuard;

    @Override
    public ReportResponse create(ReportCreateRequest req) {
        Hotel hotel = accessGuard.requireBranchManagerOrAbove(req.getHotelId());
        Report entity = reportMapper.toEntity(req);
        entity.setHotel(hotel);
        return reportMapper.toResponse(reportRepository.save(entity));
    }

    @Override
    public ReportResponse update(Long id, ReportUpdateRequest req) {
        Report entity = reportRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getHotel().getId());
        reportMapper.updateEntity(entity, req);
        return reportMapper.toResponse(reportRepository.save(entity));
    }

    @Override
    public ReportResponse getById(Long id) {
        Report entity = reportRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getHotel().getId());
        return reportMapper.toResponse(entity);
    }

    @Override
    public PageResponse<ReportResponse> getAll(int page, int size) {
        accessGuard.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ReportResponse> data = reportRepository.findAll(pageable).map(reportMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Report entity = reportRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
        accessGuard.requireBranchManagerOrAbove(entity.getHotel().getId());
        reportRepository.delete(entity);
    }

    @Override
    public List<RevenueByDateDTO> getRevenueByDate(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return bookingRepository.revenueByDate(hotelId, BookingStatus.COMPLETED, from, to);
    }

    @Override
    public List<RevenueByMonthDTO> getRevenueByMonth(Long hotelId, Integer year) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return bookingRepository.revenueByMonth(hotelId, BookingStatus.COMPLETED, year);
    }

    @Override
    public List<RevenueByYearDTO> getRevenueByYear(Long hotelId) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return bookingRepository.revenueByYear(hotelId, BookingStatus.COMPLETED);
    }

    @Override
    public List<RevenueByMonthDTO> getRevenueLast5Months(Long hotelId) {
        accessGuard.requireBranchManagerOrAbove(hotelId);

        List<YearMonth> last5Months = last5Months();
        Map<String, RevenueByMonthDTO> revenueMap = new HashMap<>();

        for (Integer year : yearsOf(last5Months)) {
            for (RevenueByMonthDTO dto : bookingRepository.revenueByMonth(hotelId, BookingStatus.COMPLETED, year)) {
                revenueMap.put(dto.getYear() + "-" + dto.getMonth(), dto);
            }
        }

        return fillMissingMonths(last5Months, revenueMap);
    }

    @Override
    public List<RevenueByWeekDTO> getRevenueByWeek(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        List<RevenueByDateDTO> dailyRevenue =
                bookingRepository.revenueByDateRange(hotelId, BookingStatus.COMPLETED, from, to);
        return groupByWeek(dailyRevenue)
                .sorted(Comparator.comparing(RevenueByWeekDTO::getYear).thenComparing(RevenueByWeekDTO::getWeek))
                .toList();
    }

    @Override
    public List<RevenueByWeekDTO> getRevenueByWeekInMonth(Long hotelId, Integer year, Integer month) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<RevenueByDateDTO> dailyRevenue =
                bookingRepository.revenueByDateRange(hotelId, BookingStatus.COMPLETED, start, end);
        return groupByWeek(dailyRevenue)
                .sorted(Comparator.comparing(RevenueByWeekDTO::getWeek))
                .toList();
    }

    @Override
    public List<RevenueByDateDTO> getRevenueByDateForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        List<Long> hotelIds = accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId));
        return bookingRepository.revenueByDateForHotels(hotelIds, BookingStatus.COMPLETED, from, to);
    }

    @Override
    public List<RevenueByMonthDTO> getRevenueByMonthForChain(Long hotelChainId, Integer year) {
        List<Long> hotelIds = accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId));
        return bookingRepository.revenueByMonthForHotels(hotelIds, BookingStatus.COMPLETED, year);
    }

    @Override
    public List<RevenueByYearDTO> getRevenueByYearForChain(Long hotelChainId) {
        List<Long> hotelIds = accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId));
        return bookingRepository.revenueByYearForHotels(hotelIds, BookingStatus.COMPLETED);
    }

    @Override
    public List<RevenueByMonthDTO> getRevenueLast5MonthsForChain(Long hotelChainId) {
        List<Long> hotelIds = accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId));

        List<YearMonth> last5Months = last5Months();
        Map<String, RevenueByMonthDTO> revenueMap = new HashMap<>();

        for (Integer year : yearsOf(last5Months)) {
            for (RevenueByMonthDTO dto :
                    bookingRepository.revenueByMonthForHotels(hotelIds, BookingStatus.COMPLETED, year)) {
                revenueMap.put(dto.getYear() + "-" + dto.getMonth(), dto);
            }
        }

        return fillMissingMonths(last5Months, revenueMap);
    }

    @Override
    public List<RevenueByWeekDTO> getRevenueByWeekForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        List<Long> hotelIds = accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId));
        List<RevenueByDateDTO> dailyRevenue =
                bookingRepository.revenueByDateRangeForHotels(hotelIds, BookingStatus.COMPLETED, from, to);
        return groupByWeek(dailyRevenue)
                .sorted(Comparator.comparing(RevenueByWeekDTO::getYear).thenComparing(RevenueByWeekDTO::getWeek))
                .toList();
    }

    @Override
    public List<RevenueByWeekDTO> getRevenueByWeekInMonthForChain(Long hotelChainId, Integer year, Integer month) {
        List<Long> hotelIds = accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId));
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<RevenueByDateDTO> dailyRevenue =
                bookingRepository.revenueByDateRangeForHotels(hotelIds, BookingStatus.COMPLETED, start, end);
        return groupByWeek(dailyRevenue)
                .sorted(Comparator.comparing(RevenueByWeekDTO::getWeek))
                .toList();
    }

    // ---------------- detailed reports ----------------

    @Override
    public OccupancyResponse getOccupancy(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return occupancy(List.of(hotelId), from, to);
    }

    @Override
    public OccupancyResponse getOccupancyForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return occupancy(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    @Override
    public List<OccupancyByDateDTO> getOccupancyByDate(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        requireRange(from, to);
        long days = ChronoUnit.DAYS.between(from, to);
        if (days > MAX_DAILY_RANGE) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Khoảng ngày quá dài, tối đa " + MAX_DAILY_RANGE + " ngày");
        }

        List<Long> hotelIds = List.of(hotelId);
        long totalRooms = roomRepository.countByHotel_IdIn(hotelIds);
        List<BookedRoomNightRow> rows = bookingRoomRepository.findBookedRoomNights(hotelIds, from, to);

        return from.datesUntil(to)
                .map(date -> {
                    long occupied = rows.stream()
                            .filter(r -> !date.isBefore(r.getCheckinDate()) && date.isBefore(r.getCheckoutDate()))
                            .map(BookedRoomNightRow::getRoomId)
                            .distinct()
                            .count();
                    return new OccupancyByDateDTO(date, totalRooms, occupied, rate(occupied, totalRooms));
                })
                .toList();
    }

    @Override
    public List<RevenueByRoomTypeDTO> getRevenueByRoomType(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return revenueByRoomType(List.of(hotelId), from, to);
    }

    @Override
    public List<RevenueByRoomTypeDTO> getRevenueByRoomTypeForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return revenueByRoomType(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    @Override
    public List<RevenueByServiceDTO> getRevenueByService(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        requireRange(from, to);
        return bookingServiceRepository.revenueByService(List.of(hotelId), BookingStatus.COMPLETED, from, to);
    }

    @Override
    public List<RevenueByServiceDTO> getRevenueByServiceForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        List<Long> hotelIds = accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId));
        requireRange(from, to);
        return bookingServiceRepository.revenueByService(hotelIds, BookingStatus.COMPLETED, from, to);
    }

    @Override
    public List<TopCustomerDTO> getTopCustomers(Long hotelId, LocalDate from, LocalDate to, int limit) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return topCustomers(List.of(hotelId), from, to, limit);
    }

    @Override
    public List<TopCustomerDTO> getTopCustomersForChain(Long hotelChainId, LocalDate from, LocalDate to, int limit) {
        return topCustomers(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to, limit);
    }

    @Override
    public List<StaffPerformanceDTO> getStaffPerformance(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return staffPerformance(List.of(hotelId), from, to);
    }

    @Override
    public List<StaffPerformanceDTO> getStaffPerformanceForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return staffPerformance(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    @Override
    public CancellationRateResponse getCancellationRate(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return cancellationRate(List.of(hotelId), from, to);
    }

    @Override
    public CancellationRateResponse getCancellationRateForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return cancellationRate(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    // ---------------- revenue summary ----------------

    @Override
    public RevenueSummaryResponse getRevenueSummary(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return revenueSummary(List.of(hotelId), from, to);
    }

    @Override
    public RevenueSummaryResponse getRevenueSummaryForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return revenueSummary(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    @Override
    public List<RevenueByTourDTO> getRevenueByTour(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return revenueByTour(List.of(hotelId), from, to);
    }

    @Override
    public List<RevenueByTourDTO> getRevenueByTourForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return revenueByTour(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    private List<RevenueByTourDTO> revenueByTour(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireInclusiveRange(from, to);
        List<TourSaleRow> sales = tourBookingRepository.findSales(hotelIds, TourBookingStatus.COMPLETED, from, to);
        Map<Long, Integer> seats = new HashMap<>();
        tourRepository
                .findAllById(sales.stream()
                        .map(TourSaleRow::getTourId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .forEach(tour -> seats.put(tour.getId(), tour.getMaxGuests()));
        return RevenueFigures.byTour(sales, seats);
    }

    @Override
    public List<BranchPerformanceDTO> getBranchPerformanceForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        requireChainAccess(hotelChainId);
        requireInclusiveRange(from, to);
        RevenueFigures.Period previous = RevenueFigures.previousPeriod(from, to);
        List<BranchPerformanceDTO> branches = hotelRepository.findAllByHotelChain_Id(hotelChainId).stream()
                .map(hotel -> {
                    List<Long> ids = List.of(hotel.getId());
                    long rooms = roomRepository.countByHotel_IdIn(ids);
                    BranchPerformanceDTO branch = RevenueFigures.branch(
                            hotel.getId(),
                            hotel.getName(),
                            periodRows(ids, rooms, from, to),
                            periodRows(ids, rooms, previous.from(), previous.to()));
                    List<FeedbackRow> reviews = feedbackRepository.findForReport(ids, from, to);
                    branch.setReviews(reviews.size());
                    branch.setAverageRating(GuestFigures.average(reviews));
                    return branch;
                })
                .toList();
        return RevenueFigures.rankBranches(branches);
    }

    @Override
    public RevenueOutlookResponse getRevenueOutlook(Long hotelId, int days) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return revenueOutlook(List.of(hotelId), days);
    }

    @Override
    public RevenueOutlookResponse getRevenueOutlookForChain(Long hotelChainId, int days) {
        return revenueOutlook(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), days);
    }

    @Override
    public List<BranchOutlookDTO> getBranchOutlookForChain(Long hotelChainId, int days) {
        requireChainAccess(hotelChainId);
        return hotelRepository.findAllByHotelChain_Id(hotelChainId).stream()
                .map(hotel -> {
                    RevenueOutlookResponse outlook = revenueOutlook(List.of(hotel.getId()), days);
                    return BranchOutlookDTO.builder()
                            .hotelId(hotel.getId())
                            .hotelName(hotel.getName())
                            .totalRooms(outlook.getTotalRooms())
                            .bookedRoomNights(outlook.getBookedRoomNights())
                            .availableRoomNights(outlook.getAvailableRoomNights())
                            .occupancyOnTheBooks(outlook.getOccupancyOnTheBooks())
                            .lastYearOccupancy(outlook.getLastYearOccupancy())
                            .totalExpected(outlook.getTotalExpected())
                            .build();
                })
                .sorted(Comparator.comparingDouble(BranchOutlookDTO::getOccupancyOnTheBooks))
                .toList();
    }

    @Override
    public List<RoomPerformanceDTO> getRoomPerformance(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        requireInclusiveRange(from, to);
        List<Long> hotelIds = List.of(hotelId);
        List<RevenueFigures.RoomInfo> rooms = roomRepository.findAllByHotel_Id(hotelId).stream()
                .map(room -> new RevenueFigures.RoomInfo(
                        room.getId(),
                        room.getRoomNumber(),
                        room.getRoomType() == null ? null : room.getRoomType().getName(),
                        room.getPrice()))
                .toList();
        return RevenueFigures.rooms(
                rooms,
                bookingRoomRepository.findBookedRoomNights(hotelIds, from, to.plusDays(1)),
                bookingRoomRepository.findRoomNightsByStatus(hotelIds, BookingStatus.COMPLETED, from, to),
                servicesByBooking(hotelIds, from, to),
                from,
                to);
    }

    /** Service lines per completed booking checking out in [from, to], to take them out of the room revenue. */
    private Map<Long, BigDecimal> servicesByBooking(List<Long> hotelIds, LocalDate from, LocalDate to) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        bookingServiceRepository
                .serviceTotalsByBooking(hotelIds, BookingStatus.COMPLETED, from, to)
                .forEach(row -> totals.put(row.getBookingId(), row.getAmount()));
        return totals;
    }

    @Override
    public List<WeekdayStatsDTO> getWeekdayPattern(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return weekdayPattern(List.of(hotelId), from, to);
    }

    @Override
    public List<WeekdayStatsDTO> getWeekdayPatternForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return weekdayPattern(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    private List<WeekdayStatsDTO> weekdayPattern(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireInclusiveRange(from, to);
        Map<LocalDate, BigDecimal> revenueByDate = new HashMap<>();
        bookingRepository
                .revenueByDateRangeForHotels(hotelIds, BookingStatus.COMPLETED, from, to)
                .forEach(day -> revenueByDate.merge(day.getDate(), day.getTotalRevenue(), BigDecimal::add));
        tourBookingRepository
                .findSales(hotelIds, TourBookingStatus.COMPLETED, from, to)
                .forEach(tour -> revenueByDate.merge(tour.getTourDate(), tour.tourRevenue(), BigDecimal::add));
        return RevenueFigures.weekdays(
                from,
                to,
                roomRepository.countByHotel_IdIn(hotelIds),
                bookingRoomRepository.findBookedRoomNights(hotelIds, from, to.plusDays(1)),
                revenueByDate);
    }

    // ---------------- monthly revenue goals ----------------

    @Override
    @Transactional
    public TargetProgressResponse setRevenueTarget(RevenueTargetRequest req) {
        Hotel hotel = accessGuard.requireBranchManagerOrAbove(req.getHotelId());
        YearMonth month = parseMonth(req.getMonth());
        RevenueTarget target = revenueTargetRepository
                .findByHotel_IdAndMonth(hotel.getId(), month.atDay(1))
                .orElse(null);
        if (req.getAmount().signum() == 0) {
            if (target != null) {
                revenueTargetRepository.delete(target);
            }
        } else {
            if (target == null) {
                target = RevenueTarget.builder()
                        .hotel(hotel)
                        .month(month.atDay(1))
                        .build();
            } else if (target.getAmount().compareTo(req.getAmount()) != 0) {
                // a new goal deserves its own alerts
                target.setReachedNotified(false);
                target.setAtRiskNotified(false);
            }
            target.setAmount(req.getAmount());
            revenueTargetRepository.save(target);
        }
        return targetProgress(hotel, month);
    }

    @Override
    public TargetProgressResponse getTargetProgress(Long hotelId, String month) {
        return targetProgress(accessGuard.requireBranchManagerOrAbove(hotelId), parseMonth(month));
    }

    @Override
    public ChainTargetProgressResponse getTargetProgressForChain(Long hotelChainId, String month) {
        requireChainAccess(hotelChainId);
        YearMonth yearMonth = parseMonth(month);
        List<TargetProgressResponse> branches = hotelRepository.findAllByHotelChain_Id(hotelChainId).stream()
                .map(hotel -> targetProgress(hotel, yearMonth))
                .toList();
        return new ChainTargetProgressResponse(
                RevenueFigures.chainTarget(branches, yearMonth, LocalDate.now()), branches);
    }

    @Override
    @Transactional
    public int sendRevenueTargetAlerts() {
        YearMonth month = YearMonth.now();
        int sent = 0;
        for (RevenueTarget target : revenueTargetRepository.findAllByMonth(month.atDay(1))) {
            Hotel hotel = target.getHotel();
            TargetProgressResponse progress = targetProgress(hotel, month);
            RevenueFigures.TargetAlert alert =
                    RevenueFigures.targetAlert(progress, target.isReachedNotified(), target.isAtRiskNotified());
            if (alert == RevenueFigures.TargetAlert.NONE) {
                continue;
            }
            List<Account> recipients = new ArrayList<>();
            recipients.add(hotel.getHotelChain().getAccount());
            if (hotel.getAccount() != null
                    && !hotel.getAccount().getId().equals(recipients.get(0).getId())) {
                recipients.add(hotel.getAccount());
            }
            if (alert == RevenueFigures.TargetAlert.REACHED) {
                notificationService.notifyAccounts(
                        recipients,
                        NotificationType.REVENUE_TARGET_REACHED,
                        NotificationMessages.revenueTargetReached(
                                hotel.getName(), month, progress.getRevenue(), progress.getTarget()),
                        null,
                        hotel.getId());
                target.setReachedNotified(true);
            } else {
                notificationService.notifyAccounts(
                        recipients,
                        NotificationType.REVENUE_TARGET_AT_RISK,
                        NotificationMessages.revenueTargetAtRisk(
                                hotel.getName(),
                                month,
                                progress.getProjectedRevenue(),
                                progress.getTarget(),
                                progress.getProjectedRate()),
                        null,
                        hotel.getId());
                target.setAtRiskNotified(true);
            }
            revenueTargetRepository.save(target);
            sent++;
        }
        return sent;
    }

    @Override
    @Transactional
    public int runRevenueTargetAlertsNow() {
        accessGuard.requireAdmin();
        return sendRevenueTargetAlerts();
    }

    @Override
    @Transactional
    public int sendWeeklyDigests() {
        LocalDate from = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(1);
        LocalDate to = from.plusDays(6);
        int sent = 0;
        for (HotelChain chain : hotelChainRepository.findAllByApprovalStatus(ApprovalStatus.APPROVED)) {
            List<Hotel> hotels = hotelRepository.findAllByHotelChain_Id(chain.getId());
            if (hotels.isEmpty()) {
                continue;
            }
            Account owner = chain.getAccount();
            List<Long> chainHotelIds = hotels.stream().map(Hotel::getId).toList();
            if (sendDigest(owner, chain.getName(), chainHotelIds, from, to, chain.getId())) {
                sent++;
            }
            for (Hotel hotel : hotels) {
                Account manager = hotel.getAccount();
                if (manager != null
                        && !manager.getId().equals(owner.getId())
                        && sendDigest(manager, hotel.getName(), List.of(hotel.getId()), from, to, hotel.getId())) {
                    sent++;
                }
            }
        }
        return sent;
    }

    @Override
    @Transactional
    public int runWeeklyDigestsNow() {
        accessGuard.requireAdmin();
        return sendWeeklyDigests();
    }

    /** One digest of [from, to] against the week before; false (nothing sent) for two quiet weeks. */
    private boolean sendDigest(
            Account recipient, String scopeName, List<Long> hotelIds, LocalDate from, LocalDate to, Long referenceId) {
        long rooms = roomRepository.countByHotel_IdIn(hotelIds);
        RevenueSummaryResponse summary = RevenueFigures.summary(
                periodRows(hotelIds, rooms, from, to),
                periodRows(hotelIds, rooms, from.minusWeeks(1), to.minusWeeks(1)));
        if (RevenueFigures.quiet(summary)) {
            return false;
        }
        RevenuePeriodDTO week = summary.getCurrent();
        notificationService.notify(
                recipient,
                NotificationType.WEEKLY_REPORT,
                NotificationMessages.weeklyReport(
                        scopeName,
                        from,
                        to,
                        week.getTotalRevenue(),
                        summary.getRevenueGrowth(),
                        week.getOccupancyRate(),
                        week.getCompletedBookings(),
                        week.getCanceledBookings()),
                null,
                referenceId);
        return true;
    }

    @Override
    public List<TrendBucketDTO> getRevenueTrend(Long hotelId, LocalDate from, LocalDate to, String granularity) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return revenueTrend(List.of(hotelId), from, to, granularity);
    }

    @Override
    public List<TrendBucketDTO> getRevenueTrendForChain(
            Long hotelChainId, LocalDate from, LocalDate to, String granularity) {
        return revenueTrend(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to, granularity);
    }

    private List<TrendBucketDTO> revenueTrend(List<Long> hotelIds, LocalDate from, LocalDate to, String granularity) {
        requireInclusiveRange(from, to);
        RevenueFigures.TrendGranularity size;
        try {
            size = RevenueFigures.TrendGranularity.valueOf(granularity.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "granularity phải là day, week, month hoặc year");
        }
        if (size == RevenueFigures.TrendGranularity.DAY && ChronoUnit.DAYS.between(from, to) >= MAX_TREND_DAYS) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Xem theo ngày tối đa " + MAX_TREND_DAYS + " ngày, hãy chọn tuần hoặc tháng");
        }
        return RevenueFigures.withPayroll(
                RevenueFigures.trend(
                        from,
                        to,
                        size,
                        bookingRepository.revenueByDateRangeForHotels(hotelIds, BookingStatus.COMPLETED, from, to),
                        tourBookingRepository.findSales(hotelIds, TourBookingStatus.COMPLETED, from, to)),
                payrollRepository.findCosts(
                        hotelIds,
                        EnumSet.of(PayrollStatus.APPROVED, PayrollStatus.PAID),
                        YearMonth.from(from).atDay(1),
                        YearMonth.from(to).atEndOfMonth()));
    }

    @Override
    public List<ReceivableDTO> getReceivables(Long hotelId) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return receivables(List.of(hotelId));
    }

    @Override
    public List<ReceivableDTO> getReceivablesForChain(Long hotelChainId) {
        return receivables(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)));
    }

    private List<ReceivableDTO> receivables(List<Long> hotelIds) {
        LocalDate today = LocalDate.now();
        List<ReceivableDTO> rows =
                bookingRepository.findReceivables(hotelIds, BookingStatus.COMPLETED, PaymentStatus.PAID);
        rows.forEach(row -> row.setDaysOverdue(Math.max(0, ChronoUnit.DAYS.between(row.getCheckoutDate(), today))));
        return rows;
    }

    @Override
    public List<GuestOriginDTO> getGuestOrigins(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return guestOrigins(List.of(hotelId), from, to);
    }

    @Override
    public List<GuestOriginDTO> getGuestOriginsForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return guestOrigins(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    private List<GuestOriginDTO> guestOrigins(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireInclusiveRange(from, to);
        return GuestFigures.origins(
                bookingRepository.findGuestOrigins(hotelIds, BookingStatus.COMPLETED, from, to),
                tourBookingRepository.findGuestOrigins(hotelIds, TourBookingStatus.COMPLETED, from, to));
    }

    @Override
    public RatingReportResponse getRatings(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return ratings(List.of(hotelId), from, to);
    }

    @Override
    public RatingReportResponse getRatingsForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return ratings(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    private RatingReportResponse ratings(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireInclusiveRange(from, to);
        RevenueFigures.Period previous = RevenueFigures.previousPeriod(from, to);
        return GuestFigures.ratings(
                from,
                to,
                feedbackRepository.findForReport(hotelIds, from, to),
                feedbackRepository.findForReport(hotelIds, previous.from(), previous.to()),
                bookingRepository
                        .findSales(hotelIds, BookingStatus.COMPLETED, from, to)
                        .size());
    }

    @Override
    public AiAssistantReportResponse getAiAssistantReport(Long hotelId, LocalDate from, LocalDate to) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        return aiAssistantReport(List.of(hotelId), from, to);
    }

    @Override
    public AiAssistantReportResponse getAiAssistantReportForChain(Long hotelChainId, LocalDate from, LocalDate to) {
        return aiAssistantReport(accessGuard.hotelIdsForChain(requireChainAccess(hotelChainId)), from, to);
    }

    private AiAssistantReportResponse aiAssistantReport(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireInclusiveRange(from, to);
        Date start = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return ChatFigures.assistant(
                chatMessageRepository.findForReport(hotelIds, start, end),
                tourBookingRepository.countPlacedByAi(hotelIds, start, end),
                tourBookingRepository.findSales(hotelIds, TourBookingStatus.COMPLETED, from, to),
                conversationRepository.countByHotel_IdInAndNeedsStaffTrue(hotelIds));
    }

    /** Revenue of the month so far plus what is booked for the rest of it, against the branch's goal. */
    private TargetProgressResponse targetProgress(Hotel hotel, YearMonth month) {
        List<Long> ids = List.of(hotel.getId());
        LocalDate today = LocalDate.now();
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        BigDecimal revenue = BigDecimal.ZERO;
        if (!start.isAfter(today)) {
            LocalDate upTo = end.isBefore(today) ? end : today;
            revenue = RevenueFigures.sum(bookingRepository.findSales(ids, BookingStatus.COMPLETED, start, upTo).stream()
                            .map(CompletedBookingRow::getTotalAmount))
                    .add(RevenueFigures.sum(
                            tourBookingRepository.findSales(ids, TourBookingStatus.COMPLETED, start, upTo).stream()
                                    .map(TourSaleRow::tourRevenue)));
        }
        BigDecimal onTheBooks = BigDecimal.ZERO;
        if (!end.isBefore(today)) {
            LocalDate from = start.isAfter(today) ? start : today;
            onTheBooks = RevenueFigures.outlook(
                            from,
                            end,
                            bookingRepository.findOpen(
                                    ids, EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED), end),
                            tourBookingRepository.findUpcoming(
                                    ids, EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED), from, end),
                            List.of())
                    .getTotalExpected();
        }
        BigDecimal target = revenueTargetRepository
                .findByHotel_IdAndMonth(hotel.getId(), start)
                .map(RevenueTarget::getAmount)
                .orElse(null);
        return RevenueFigures.targetProgress(hotel.getId(), hotel.getName(), month, target, revenue, onTheBooks, today);
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "month phải có dạng yyyy-MM");
        }
    }

    /** Today and the next days - 1 days. */
    private RevenueOutlookResponse revenueOutlook(List<Long> hotelIds, int days) {
        if (days < 1 || days > MAX_OUTLOOK_DAYS) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số ngày dự kiến phải từ 1 đến " + MAX_OUTLOOK_DAYS);
        }
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(days - 1L);
        RevenueOutlookResponse outlook = RevenueFigures.outlook(
                from,
                to,
                bookingRepository.findOpen(hotelIds, EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED), to),
                tourBookingRepository.findUpcoming(
                        hotelIds, EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED), from, to),
                bookingRepository.findUnpaid(hotelIds, BookingStatus.COMPLETED, PaymentStatus.PAID));
        RevenueFigures.occupancyOnTheBooks(
                outlook,
                roomRepository.countByHotel_IdIn(hotelIds),
                bookingRoomRepository.findBookedRoomNights(hotelIds, from, to.plusDays(1)),
                bookingRoomRepository.findBookedRoomNights(
                        hotelIds, from.minusYears(1), to.minusYears(1).plusDays(1)));
        return outlook;
    }

    /** [from, to] against its {@link RevenueFigures#previousPeriod previous period}. */
    private RevenueSummaryResponse revenueSummary(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireInclusiveRange(from, to);
        long totalRooms = roomRepository.countByHotel_IdIn(hotelIds);
        RevenueFigures.Period previous = RevenueFigures.previousPeriod(from, to);
        RevenueFigures.PeriodRows current = periodRows(hotelIds, totalRooms, from, to);
        RevenueSummaryResponse summary =
                RevenueFigures.summary(current, periodRows(hotelIds, totalRooms, previous.from(), previous.to()));
        summary.setByCustomerType(RevenueFigures.byCustomerType(
                current.bookings(),
                Set.copyOf(bookingRepository.findCustomerIdsBefore(hotelIds, BookingStatus.COMPLETED, from))));
        RevenueFigures.PaymentTiming timing = RevenueFigures.paymentTiming(
                bookingRepository.findPaymentTiming(hotelIds, BookingStatus.COMPLETED, from, to));
        summary.setByPaymentTiming(timing.byTiming());
        summary.setAverageDaysToCollect(timing.averageDaysToCollect());
        return summary;
    }

    private RevenueFigures.PeriodRows periodRows(List<Long> hotelIds, long totalRooms, LocalDate from, LocalDate to) {
        LocalDate end = to.plusDays(1);
        long occupiedRoomNights = bookingRoomRepository.findBookedRoomNights(hotelIds, from, end).stream()
                .mapToLong(r -> nightsWithin(r, from, end))
                .sum();
        BigDecimal serviceRevenue = RevenueFigures.sum(
                bookingServiceRepository.revenueByService(hotelIds, BookingStatus.COMPLETED, from, to).stream()
                        .map(RevenueByServiceDTO::getTotalRevenue));
        return new RevenueFigures.PeriodRows(
                from,
                to,
                bookingRepository.findSales(hotelIds, BookingStatus.COMPLETED, from, to),
                serviceRevenue,
                tourBookingRepository.findSales(hotelIds, TourBookingStatus.COMPLETED, from, to),
                bookingRoomRepository.findRoomNightsByStatus(hotelIds, BookingStatus.COMPLETED, from, to),
                totalRooms,
                occupiedRoomNights,
                bookingRepository.countBookings(hotelIds, from, to),
                bookingRepository.countBookingsByStatus(hotelIds, BookingStatus.CANCELED, from, to),
                bookingRepository.sumAmountByStatus(hotelIds, BookingStatus.CANCELED, from, to),
                RevenueFigures.payrollCost(
                        payrollRepository.findCosts(
                                hotelIds,
                                EnumSet.of(PayrollStatus.APPROVED, PayrollStatus.PAID),
                                YearMonth.from(from).atDay(1),
                                YearMonth.from(to).atEndOfMonth()),
                        from,
                        to),
                bookingServiceRepository.serviceTotalsByBooking(hotelIds, BookingStatus.COMPLETED, from, to).stream()
                        .filter(row ->
                                row.getAmount() != null && row.getAmount().signum() > 0)
                        .count());
    }

    private OccupancyResponse occupancy(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireRange(from, to);
        long nights = ChronoUnit.DAYS.between(from, to);
        long totalRooms = roomRepository.countByHotel_IdIn(hotelIds);
        long roomNights = totalRooms * nights;

        long occupiedRoomNights = bookingRoomRepository.findBookedRoomNights(hotelIds, from, to).stream()
                .mapToLong(r -> nightsWithin(r, from, to))
                .sum();

        return OccupancyResponse.builder()
                .from(from)
                .to(to)
                .totalRooms(totalRooms)
                .roomNights(roomNights)
                .occupiedRoomNights(occupiedRoomNights)
                .occupancyRate(rate(occupiedRoomNights, roomNights))
                .build();
    }

    private List<RevenueByRoomTypeDTO> revenueByRoomType(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireRange(from, to);
        List<BookedRoomNightRow> rows =
                bookingRoomRepository.findRoomNightsByStatus(hotelIds, BookingStatus.COMPLETED, from, to);
        return RevenueFigures.roomTypes(
                from,
                to,
                roomRepository.countByRoomType(hotelIds),
                rows,
                RevenueFigures.roomRevenue(rows, servicesByBooking(hotelIds, from, to)),
                bookingRoomRepository.findBookedRoomNights(hotelIds, from, to.plusDays(1)));
    }

    private List<TopCustomerDTO> topCustomers(List<Long> hotelIds, LocalDate from, LocalDate to, int limit) {
        requireRange(from, to);
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        // every guest's bookings, not just the top ones: tours can lift someone into the top
        return RevenueFigures.topCustomers(
                bookingRepository.topCustomers(hotelIds, BookingStatus.COMPLETED, from, to, Pageable.unpaged()),
                tourBookingRepository.findByCustomer(hotelIds, TourBookingStatus.COMPLETED, from, to),
                safeLimit);
    }

    private List<StaffPerformanceDTO> staffPerformance(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireRange(from, to);
        List<StaffPerformanceDTO> rows = RevenueFigures.staff(
                bookingRepository.staffPerformance(hotelIds, from, to),
                bookingRepository.findSales(hotelIds, BookingStatus.COMPLETED, from, to),
                tourBookingRepository.findHandledByStaff(hotelIds, TourBookingStatus.COMPLETED, from, to));

        // the query can only see the Account; names/positions live on Employee (managers have none)
        for (StaffPerformanceDTO row : rows) {
            if (row.getUsername() == null) {
                accountRepository
                        .findById(row.getAccountId())
                        .ifPresent(account -> row.setUsername(account.getUsername()));
            }
            Employee employee = employeeRepository.findByAccount_Id(row.getAccountId());
            if (employee != null) {
                row.setFullName(employee.getFullName());
                if (employee.getPosition() != null) {
                    row.setPositionName(employee.getPosition().getName());
                }
            }
        }
        return rows;
    }

    private CancellationRateResponse cancellationRate(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireRange(from, to);
        long total = bookingRepository.countBookings(hotelIds, from, to);
        long canceled = bookingRepository.countBookingsByStatus(hotelIds, BookingStatus.CANCELED, from, to);
        long completed = bookingRepository.countBookingsByStatus(hotelIds, BookingStatus.COMPLETED, from, to);
        long canceledAtDesk = bookingRepository.countTakenAtDeskByStatus(hotelIds, BookingStatus.CANCELED, from, to);
        RevenueFigures.TourCancellations tours =
                RevenueFigures.tourCancellations(tourBookingRepository.countByStatus(hotelIds, from, to));
        RevenueFigures.CancellationLeadTimes leadTimes =
                RevenueFigures.cancellationLeadTimes(bookingRepository.findCanceled(hotelIds, from, to));

        return CancellationRateResponse.builder()
                .from(from)
                .to(to)
                .totalBooking(total)
                .canceledBooking(canceled)
                .completedBooking(completed)
                .cancellationRate(rate(canceled, total))
                .canceledRevenue(bookingRepository.sumAmountByStatus(hotelIds, BookingStatus.CANCELED, from, to))
                .canceledFrontDesk(canceledAtDesk)
                .canceledSelfService(canceled - canceledAtDesk)
                .tourOrders(tours.orders())
                .tourCanceled(tours.canceled())
                .tourCanceledByGuest(tours.byGuest())
                .tourCanceledByHotel(tours.byHotel())
                .tourCancellationRate(tours.rate())
                .byLeadTime(leadTimes.byLeadTime())
                .medianCancelLeadDays(leadTimes.medianDays())
                .lateCancellations(leadTimes.late())
                .lateCancellationShare(leadTimes.lateShare())
                .canceledByGuest(leadTimes.byGuest())
                .canceledByHotel(leadTimes.byHotel())
                .build();
    }

    /** Nights of this stay that fall inside [from, to) - stays can start before or end after it. */
    private long nightsWithin(BookedRoomNightRow row, LocalDate from, LocalDate to) {
        LocalDate start = row.getCheckinDate().isBefore(from) ? from : row.getCheckinDate();
        LocalDate end = row.getCheckoutDate().isAfter(to) ? to : row.getCheckoutDate();
        return Math.max(0, ChronoUnit.DAYS.between(start, end));
    }

    private double rate(long part, long whole) {
        if (whole <= 0) {
            return 0d;
        }
        return Math.round((part * 10000d) / whole) / 100d;
    }

    private void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "'from' phải trước 'to'");
        }
    }

    /** Both ends inclusive, so a single day is a valid period. */
    private void requireInclusiveRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "'from' không được sau 'to'");
        }
    }

    private Long requireChainAccess(Long hotelChainId) {
        accessGuard.requireChainOwner(hotelChainId);
        return hotelChainId;
    }

    private List<YearMonth> last5Months() {
        LocalDate now = LocalDate.now();
        // Lấy danh sách 5 tháng gần nhất (kể cả tháng hiện tại)
        return IntStream.rangeClosed(0, 4)
                .mapToObj(i -> YearMonth.from(now.minusMonths(i)))
                .sorted()
                .toList();
    }

    private Set<Integer> yearsOf(List<YearMonth> months) {
        // Gom các năm cần query (có thể qua 2 năm)
        return months.stream().map(YearMonth::getYear).collect(Collectors.toSet());
    }

    private List<RevenueByMonthDTO> fillMissingMonths(
            List<YearMonth> months, Map<String, RevenueByMonthDTO> revenueMap) {
        // Trả về đúng số tháng yêu cầu, tháng không có booking thì = 0
        return months.stream()
                .map(ym -> {
                    String key = ym.getYear() + "-" + ym.getMonthValue();
                    return revenueMap.getOrDefault(
                            key, new RevenueByMonthDTO(ym.getYear(), ym.getMonthValue(), BigDecimal.ZERO, 0L));
                })
                .toList();
    }

    private java.util.stream.Stream<RevenueByWeekDTO> groupByWeek(List<RevenueByDateDTO> dailyRevenue) {
        Map<YearWeek, List<RevenueByDateDTO>> grouped =
                dailyRevenue.stream().collect(Collectors.groupingBy(dto -> YearWeek.from(dto.getDate())));

        return grouped.entrySet().stream().map(entry -> {
            YearWeek yw = entry.getKey();
            List<RevenueByDateDTO> list = entry.getValue();

            BigDecimal totalRevenue =
                    list.stream().map(RevenueByDateDTO::getTotalRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);

            long totalBooking =
                    list.stream().mapToLong(RevenueByDateDTO::getTotalBooking).sum();

            return new RevenueByWeekDTO(
                    yw.getYear(), yw.getWeek(), yw.getStartDate(), yw.getEndDate(), totalRevenue, totalBooking);
        });
    }
}
