package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
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
import com.vanmo.booking_app_be.entity.Report;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.ReportMapper;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.ReportRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.ReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final BookingRepository bookingRepository;
    private final AccessGuard accessGuard;

    @Override
    public ReportResponse create(ReportCreateRequest req) {
        Report entity = reportMapper.toEntity(req);
        return reportMapper.toResponse(reportRepository.save(entity));
    }

    @Override
    public ReportResponse update(Long id, ReportUpdateRequest req) {
        Report entity = reportRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
        reportMapper.updateEntity(entity, req);
        return reportMapper.toResponse(reportRepository.save(entity));
    }

    @Override
    public ReportResponse getById(Long id) {
        Report entity = reportRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
        return reportMapper.toResponse(entity);
    }

    @Override
    public PageResponse<ReportResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ReportResponse> data = reportRepository.findAll(pageable).map(reportMapper::toResponse);
        return PageResponse.from(data);
    }

    @Override
    public void delete(Long id) {
        Report entity = reportRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found id=" + id));
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
