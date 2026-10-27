package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.ReportCreateRequest;
import com.vanmo.booking_app_be.dto.request.ReportUpdateRequest;
import com.vanmo.booking_app_be.dto.response.*;
import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Report;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.entity.enums.PayrollStatus;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.ReportMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.BookingRoomRepository;
import com.vanmo.booking_app_be.repository.BookingServiceRepository;
import com.vanmo.booking_app_be.repository.EmployeeRepository;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.PayrollRepository;
import com.vanmo.booking_app_be.repository.ReportRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.TourBookingRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.ReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int MAX_DAILY_RANGE = 92;
    private static final int MAX_OUTLOOK_DAYS = 90;

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final RoomRepository roomRepository;
    private final EmployeeRepository employeeRepository;
    private final HotelRepository hotelRepository;
    private final TourBookingRepository tourBookingRepository;
    private final PayrollRepository payrollRepository;
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
        return RevenueFigures.byTour(tourBookingRepository.findSales(hotelIds, TourBookingStatus.COMPLETED, from, to));
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
                    return RevenueFigures.branch(
                            hotel.getId(),
                            hotel.getName(),
                            periodRows(ids, rooms, from, to),
                            periodRows(ids, rooms, previous.from(), previous.to()));
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
                from,
                to);
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

    /** Today and the next days - 1 days. */
    private RevenueOutlookResponse revenueOutlook(List<Long> hotelIds, int days) {
        if (days < 1 || days > MAX_OUTLOOK_DAYS) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số ngày dự kiến phải từ 1 đến " + MAX_OUTLOOK_DAYS);
        }
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(days - 1L);
        return RevenueFigures.outlook(
                from,
                to,
                bookingRepository.findOpen(hotelIds, EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED), to),
                tourBookingRepository.findUpcoming(
                        hotelIds, EnumSet.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED), from, to),
                bookingRepository.findUnpaid(hotelIds, BookingStatus.COMPLETED, PaymentStatus.PAID));
    }

    /** [from, to] against its {@link RevenueFigures#previousPeriod previous period}. */
    private RevenueSummaryResponse revenueSummary(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireInclusiveRange(from, to);
        long totalRooms = roomRepository.countByHotel_IdIn(hotelIds);
        RevenueFigures.Period previous = RevenueFigures.previousPeriod(from, to);
        return RevenueFigures.summary(
                periodRows(hotelIds, totalRooms, from, to),
                periodRows(hotelIds, totalRooms, previous.from(), previous.to()));
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
                        to));
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

        Map<Long, RevenueByRoomTypeDTO> byType = new LinkedHashMap<>();
        for (BookedRoomNightRow row : rows) {
            long nights = Math.max(1, ChronoUnit.DAYS.between(row.getCheckinDate(), row.getCheckoutDate()));
            BigDecimal revenue = row.getRoomPrice() == null
                    ? BigDecimal.ZERO
                    : row.getRoomPrice().multiply(BigDecimal.valueOf(nights));

            RevenueByRoomTypeDTO acc = byType.computeIfAbsent(
                    row.getRoomTypeId(),
                    id -> new RevenueByRoomTypeDTO(id, row.getRoomTypeName(), 0L, 0L, BigDecimal.ZERO));
            acc.setBookedRooms(acc.getBookedRooms() + 1);
            acc.setRoomNights(acc.getRoomNights() + nights);
            acc.setTotalRevenue(acc.getTotalRevenue().add(revenue));
        }

        return byType.values().stream()
                .sorted(Comparator.comparing(RevenueByRoomTypeDTO::getTotalRevenue)
                        .reversed())
                .toList();
    }

    private List<TopCustomerDTO> topCustomers(List<Long> hotelIds, LocalDate from, LocalDate to, int limit) {
        requireRange(from, to);
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        return bookingRepository.topCustomers(
                hotelIds, BookingStatus.COMPLETED, from, to, PageRequest.of(0, safeLimit));
    }

    private List<StaffPerformanceDTO> staffPerformance(List<Long> hotelIds, LocalDate from, LocalDate to) {
        requireRange(from, to);
        List<StaffPerformanceDTO> rows = bookingRepository.staffPerformance(hotelIds, from, to);

        // the query can only see the Account; names/positions live on Employee (managers have none)
        for (StaffPerformanceDTO row : rows) {
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

        return CancellationRateResponse.builder()
                .from(from)
                .to(to)
                .totalBooking(total)
                .canceledBooking(canceled)
                .completedBooking(completed)
                .cancellationRate(rate(canceled, total))
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
