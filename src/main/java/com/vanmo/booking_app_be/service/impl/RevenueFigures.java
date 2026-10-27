package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.vanmo.booking_app_be.dto.response.BookedRoomNightRow;
import com.vanmo.booking_app_be.dto.response.BranchPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.CompletedBookingRow;
import com.vanmo.booking_app_be.dto.response.ExpectedRevenueDTO;
import com.vanmo.booking_app_be.dto.response.OpenBookingRow;
import com.vanmo.booking_app_be.dto.response.PayrollCostRow;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueShareDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.RoomPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.dto.response.WeekdayStatsDTO;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;

/** The revenue summary's arithmetic, kept apart from the queries so it can be checked on plain rows. */
final class RevenueFigures {

    static final String SELF_SERVICE = "SELF_SERVICE";
    static final String FRONT_DESK = "FRONT_DESK";
    static final String TOUR = "TOUR";
    static final String NEW_GUEST = "NEW";
    static final String RETURNING_GUEST = "RETURNING";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private RevenueFigures() {}

    /**
     * What one period of the summary is computed from.
     *
     * @param soldRooms rooms of the completed bookings (check-out in the period)
     * @param occupiedRoomNights nights of every booking that was not cancelled, cut to the period
     * @param totalBookings bookings checking in during the period
     * @param canceledRevenue what the cancelled ones among them were worth
     * @param payrollCost the payslips' part that falls in the period, see {@link #payrollCost}
     */
    record PeriodRows(
            LocalDate from,
            LocalDate to,
            List<CompletedBookingRow> bookings,
            BigDecimal serviceRevenue,
            List<TourSaleRow> tours,
            List<BookedRoomNightRow> soldRooms,
            long totalRooms,
            long occupiedRoomNights,
            long totalBookings,
            long canceledBookings,
            BigDecimal canceledRevenue,
            BigDecimal payrollCost) {

        long days() {
            return ChronoUnit.DAYS.between(from, to) + 1;
        }
    }

    /** Both ends inclusive. */
    record Period(LocalDate from, LocalDate to) {}

    /**
     * The period [from, to] is compared with. Calendar-aligned ranges compare with the same stretch one calendar unit
     * earlier, the way hotels read their books: a month or month-to-date with the same days of the previous month (a
     * whole month with the whole previous month), a range from 1 January over several months with the same range last
     * year. Any other range compares with the same number of days just before it.
     */
    static Period previousPeriod(LocalDate from, LocalDate to) {
        boolean wholeMonthEnd = to.getDayOfMonth() == to.lengthOfMonth();
        if (from.getDayOfMonth() == 1 && YearMonth.from(from).equals(YearMonth.from(to))) {
            LocalDate start = from.minusMonths(1);
            return new Period(start, wholeMonthEnd ? start.withDayOfMonth(start.lengthOfMonth()) : to.minusMonths(1));
        }
        if (from.getDayOfYear() == 1 && from.getYear() == to.getYear()) {
            LocalDate end = to.minusYears(1);
            return new Period(from.minusYears(1), wholeMonthEnd ? end.withDayOfMonth(end.lengthOfMonth()) : end);
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return new Period(from.minusDays(days), from.minusDays(1));
    }

    static RevenueSummaryResponse summary(PeriodRows currentRows, PeriodRows previousRows) {
        RevenuePeriodDTO current = period(currentRows);
        RevenuePeriodDTO previous = period(previousRows);
        return RevenueSummaryResponse.builder()
                .current(current)
                .previous(previous)
                .revenueGrowth(growth(current.getTotalRevenue(), previous.getTotalRevenue()))
                .bookingGrowth(growth(
                        BigDecimal.valueOf(current.getCompletedBookings()),
                        BigDecimal.valueOf(previous.getCompletedBookings())))
                .adrGrowth(growth(current.getAdr(), previous.getAdr()))
                .revparGrowth(growth(current.getRevpar(), previous.getRevpar()))
                // growth over a loss would read backwards, so only a previous profit is a base
                .profitGrowth(
                        previous.getGrossProfit().signum() > 0
                                ? growth(current.getGrossProfit(), previous.getGrossProfit())
                                : null)
                .occupancyChange(round2(current.getOccupancyRate() - previous.getOccupancyRate()))
                .byPaymentMethod(byPaymentMethod(currentRows.bookings()))
                .byChannel(byChannel(currentRows.bookings(), currentRows.tours()))
                .build();
    }

    static RevenuePeriodDTO period(PeriodRows rows) {
        BigDecimal bookingRevenue = sum(rows.bookings().stream().map(CompletedBookingRow::getTotalAmount));
        BigDecimal serviceRevenue = rows.serviceRevenue() == null ? BigDecimal.ZERO : rows.serviceRevenue();
        // a booking's total already holds its services; what is left is what the rooms earned
        BigDecimal roomRevenue = bookingRevenue.subtract(serviceRevenue).max(BigDecimal.ZERO);
        BigDecimal tourRevenue = sum(rows.tours().stream().map(TourSaleRow::tourRevenue));

        BigDecimal totalRevenue = bookingRevenue.add(tourRevenue);
        BigDecimal payrollCost = rows.payrollCost() == null ? BigDecimal.ZERO : rows.payrollCost();
        BigDecimal grossProfit = totalRevenue.subtract(payrollCost);

        long nightsSold =
                rows.soldRooms().stream().mapToLong(RevenueFigures::nights).sum();
        long availableNights = rows.totalRooms() * rows.days();
        long completed = rows.bookings().size();

        return RevenuePeriodDTO.builder()
                .from(rows.from())
                .to(rows.to())
                .totalRevenue(totalRevenue)
                .payrollCost(payrollCost)
                .grossProfit(grossProfit)
                .profitMargin(
                        totalRevenue.signum() == 0
                                ? 0d
                                : grossProfit
                                        .multiply(HUNDRED)
                                        .divide(totalRevenue, 2, RoundingMode.HALF_UP)
                                        .doubleValue())
                .roomRevenue(roomRevenue)
                .serviceRevenue(serviceRevenue)
                .tourRevenue(tourRevenue)
                .completedBookings(completed)
                .completedTours(rows.tours().size())
                .tourGuests(rows.tours().stream()
                        .map(TourSaleRow::getGuests)
                        .filter(Objects::nonNull)
                        .mapToLong(Integer::longValue)
                        .sum())
                .roomNightsSold(nightsSold)
                .availableRoomNights(availableNights)
                .occupancyRate(rate(rows.occupiedRoomNights(), availableNights))
                .adr(perUnit(roomRevenue, nightsSold))
                .revpar(perUnit(roomRevenue, availableNights))
                .averagePerBooking(perUnit(bookingRevenue, completed))
                .totalBookings(rows.totalBookings())
                .canceledBookings(rows.canceledBookings())
                .cancellationRate(rate(rows.canceledBookings(), rows.totalBookings()))
                .canceledRevenue(rows.canceledRevenue() == null ? BigDecimal.ZERO : rows.canceledRevenue())
                .averageStayNights(average(rows.bookings().stream()
                        .filter(b -> b.getCheckinDate() != null && b.getCheckoutDate() != null)
                        .mapToLong(b -> Math.max(1, ChronoUnit.DAYS.between(b.getCheckinDate(), b.getCheckoutDate())))))
                .averageLeadDays(average(rows.bookings().stream()
                        .filter(b -> b.getCreatedAt() != null && b.getCheckinDate() != null)
                        // a walk-in booked on arrival day (or entered late) counts as zero days ahead
                        .mapToLong(b -> Math.max(0, ChronoUnit.DAYS.between(bookedOn(b), b.getCheckinDate())))))
                .build();
    }

    /**
     * Monday to Sunday over [from, to]: each weekday's occupancy and revenue averaged over the days of the period that
     * fall on it.
     *
     * @param booked rooms of every booking that was not cancelled overlapping [from, to + 1)
     * @param revenueByDate what each day earned (completed bookings by check-out day, completed tours by tour date)
     */
    static List<WeekdayStatsDTO> weekdays(
            LocalDate from,
            LocalDate to,
            long totalRooms,
            List<BookedRoomNightRow> booked,
            Map<LocalDate, BigDecimal> revenueByDate) {
        long[] days = new long[7];
        long[] occupied = new long[7];
        BigDecimal[] revenue = new BigDecimal[7];
        Arrays.fill(revenue, BigDecimal.ZERO);
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            int index = day.getDayOfWeek().getValue() - 1;
            LocalDate date = day;
            days[index]++;
            occupied[index] += booked.stream()
                    .filter(r -> !date.isBefore(r.getCheckinDate()) && date.isBefore(r.getCheckoutDate()))
                    .map(BookedRoomNightRow::getRoomId)
                    .distinct()
                    .count();
            revenue[index] = revenue[index].add(revenueByDate.getOrDefault(day, BigDecimal.ZERO));
        }
        return IntStream.range(0, 7)
                .mapToObj(i -> new WeekdayStatsDTO(
                        i + 1,
                        days[i],
                        rate(occupied[i], totalRooms * days[i]),
                        perUnit(revenue[i], days[i]),
                        revenue[i]))
                .toList();
    }

    /** A room as the per-room report lists it. */
    record RoomInfo(Long id, String number, String typeName, BigDecimal price) {}

    /**
     * Every room of the branch over [from, to], busiest first; rooms nobody stayed in are kept (at 0) so the branch sees
     * them.
     *
     * @param booked rooms of every booking that was not cancelled overlapping [from, to + 1)
     * @param completed rooms of the completed bookings checking out in [from, to]
     */
    static List<RoomPerformanceDTO> rooms(
            List<RoomInfo> rooms,
            List<BookedRoomNightRow> booked,
            List<BookedRoomNightRow> completed,
            LocalDate from,
            LocalDate to) {
        LocalDate end = to.plusDays(1);
        long days = ChronoUnit.DAYS.between(from, end);
        Map<Long, RoomPerformanceDTO> byRoom = new LinkedHashMap<>();
        for (RoomInfo room : rooms) {
            byRoom.put(
                    room.id(),
                    new RoomPerformanceDTO(
                            room.id(), room.number(), room.typeName(), room.price(), 0L, 0d, 0L, BigDecimal.ZERO));
        }
        for (BookedRoomNightRow row : booked) {
            RoomPerformanceDTO room = byRoom.get(row.getRoomId());
            if (room != null) {
                room.setOccupiedNights(room.getOccupiedNights() + nightsWithin(row, from, end));
            }
        }
        for (BookedRoomNightRow row : completed) {
            RoomPerformanceDTO room = byRoom.get(row.getRoomId());
            if (room != null && row.getRoomPrice() != null) {
                room.setCompletedStays(room.getCompletedStays() + 1);
                room.setRevenue(room.getRevenue().add(row.getRoomPrice().multiply(BigDecimal.valueOf(nights(row)))));
            }
        }
        byRoom.values().forEach(room -> room.setOccupancyRate(rate(room.getOccupiedNights(), days)));
        return byRoom.values().stream()
                .sorted(Comparator.comparingDouble(RoomPerformanceDTO::getOccupancyRate)
                        .thenComparing(RoomPerformanceDTO::getRevenue)
                        .reversed()
                        .thenComparing(RoomPerformanceDTO::getRoomNumber, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    /** Nights of this stay inside [from, end) - stays can start before or end after it. */
    private static long nightsWithin(BookedRoomNightRow row, LocalDate from, LocalDate end) {
        LocalDate start = row.getCheckinDate().isBefore(from) ? from : row.getCheckinDate();
        LocalDate stop = row.getCheckoutDate().isAfter(end) ? end : row.getCheckoutDate();
        return Math.max(0, ChronoUnit.DAYS.between(start, stop));
    }

    /**
     * Payroll cost of [from, to]: each payslip pays for a whole month, so it is spread evenly over that month's days and
     * only the days inside the period count (a 10-day range in a 30-day month carries a third of that month's salary).
     */
    static BigDecimal payrollCost(List<PayrollCostRow> payslips, LocalDate from, LocalDate to) {
        BigDecimal cost = BigDecimal.ZERO;
        for (PayrollCostRow payslip : payslips) {
            if (payslip.getMonth() == null || payslip.getTotalSalary() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(payslip.getMonth());
            LocalDate start = month.atDay(1).isBefore(from) ? from : month.atDay(1);
            LocalDate end = month.atEndOfMonth().isAfter(to) ? to : month.atEndOfMonth();
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            if (days > 0) {
                cost = cost.add(payslip.getTotalSalary()
                        .multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(month.lengthOfMonth()), 0, RoundingMode.HALF_UP));
            }
        }
        return cost;
    }

    private static LocalDate bookedOn(CompletedBookingRow booking) {
        return booking.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Mean with one decimal; 0 for no values. */
    private static double average(LongStream values) {
        return Math.round(values.average().orElse(0) * 10d) / 10d;
    }

    /** Every payment method, even one nobody used, so the app can show a fixed list. */
    static List<RevenueShareDTO> byPaymentMethod(List<CompletedBookingRow> bookings) {
        Map<PaymentMethod, RevenueShareDTO> slices = new LinkedHashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            slices.put(method, slice(method.name()));
        }
        for (CompletedBookingRow row : bookings) {
            if (row.getPaymentMethod() != null) {
                add(slices.get(row.getPaymentMethod()), row.getTotalAmount());
            }
        }
        return withShares(slices.values());
    }

    static List<RevenueShareDTO> byChannel(List<CompletedBookingRow> bookings, List<TourSaleRow> tours) {
        RevenueShareDTO selfService = slice(SELF_SERVICE);
        RevenueShareDTO frontDesk = slice(FRONT_DESK);
        RevenueShareDTO tour = slice(TOUR);
        for (CompletedBookingRow row : bookings) {
            add(row.getHandledByAccountId() == null ? selfService : frontDesk, row.getTotalAmount());
        }
        for (TourSaleRow row : tours) {
            add(tour, row.tourRevenue());
        }
        return withShares(List.of(selfService, frontDesk, tour));
    }

    /**
     * Completed tour orders per tour, biggest earner first. Orders of a deleted tour (null id) are grouped by the name
     * they kept, so two branches' tours with the same name stay apart while they exist.
     */
    static List<RevenueByTourDTO> byTour(List<TourSaleRow> tours) {
        Map<String, RevenueByTourDTO> byKey = new LinkedHashMap<>();
        for (TourSaleRow row : tours) {
            String key = row.getTourId() != null ? "id:" + row.getTourId() : "name:" + row.getTourName();
            RevenueByTourDTO acc = byKey.computeIfAbsent(
                    key,
                    k -> new RevenueByTourDTO(
                            row.getTourId(), row.getTourName(), 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, 0L));
            acc.setOrders(acc.getOrders() + 1);
            acc.setGuests(acc.getGuests() + (row.getGuests() == null ? 0 : row.getGuests()));
            acc.setTourRevenue(acc.getTourRevenue().add(row.tourRevenue()));
            if (row.getRoomAmount() != null) {
                acc.setPackageRoomAmount(acc.getPackageRoomAmount().add(row.getRoomAmount()));
            }
            if (Boolean.TRUE.equals(row.getCreatedByAi())) {
                acc.setAiOrders(acc.getAiOrders() + 1);
            }
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(RevenueByTourDTO::getTourRevenue).reversed())
                .toList();
    }

    /** One branch's row of the chain comparison; the share is filled in by {@link #rankBranches}. */
    static BranchPerformanceDTO branch(Long hotelId, String hotelName, PeriodRows current, PeriodRows previous) {
        RevenuePeriodDTO now = period(current);
        BigDecimal before = period(previous).getTotalRevenue();
        return BranchPerformanceDTO.builder()
                .hotelId(hotelId)
                .hotelName(hotelName)
                .current(now)
                .previousRevenue(before)
                .revenueGrowth(growth(now.getTotalRevenue(), before))
                .build();
    }

    /** Fills each branch's share of the chain's revenue and puts the biggest earner first. */
    static List<BranchPerformanceDTO> rankBranches(List<BranchPerformanceDTO> branches) {
        BigDecimal total = sum(branches.stream().map(b -> b.getCurrent().getTotalRevenue()));
        for (BranchPerformanceDTO branch : branches) {
            branch.setRevenueShare(
                    total.signum() == 0
                            ? 0d
                            : branch.getCurrent()
                                    .getTotalRevenue()
                                    .multiply(HUNDRED)
                                    .divide(total, 2, RoundingMode.HALF_UP)
                                    .doubleValue());
        }
        return branches.stream()
                .sorted(Comparator.comparing(
                                (BranchPerformanceDTO b) -> b.getCurrent().getTotalRevenue())
                        .reversed()
                        .thenComparing(BranchPerformanceDTO::getHotelName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    /**
     * Revenue still to come over [from, to] and money still to collect.
     *
     * @param open open bookings checking out on or before {@code to}; earlier check-outs are overdue
     * @param tours open tour orders departing in [from, to]
     * @param unpaidCompleted completed bookings never marked as paid
     */
    static RevenueOutlookResponse outlook(
            LocalDate from,
            LocalDate to,
            List<OpenBookingRow> open,
            List<TourSaleRow> tours,
            List<OpenBookingRow> unpaidCompleted) {
        Map<LocalDate, ExpectedRevenueDTO> byDate = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDate.put(day, new ExpectedRevenueDTO(day, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L));
        }
        // slices double as count + amount accumulators
        RevenueShareDTO booked = slice("BOOKED");
        RevenueShareDTO prepaid = slice("PREPAID");
        RevenueShareDTO awaiting = slice("AWAITING");
        RevenueShareDTO overdue = slice("OVERDUE");
        for (OpenBookingRow row : open) {
            if (row.getCheckoutDate().isBefore(from)) {
                add(overdue, row.getTotalAmount());
                continue;
            }
            ExpectedRevenueDTO day = byDate.get(row.getCheckoutDate());
            if (day == null) {
                continue;
            }
            BigDecimal amount = row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount();
            day.setBookingRevenue(day.getBookingRevenue().add(amount));
            day.setBookings(day.getBookings() + 1);
            add(booked, amount);
            if (row.getPaymentStatus() == PaymentStatus.PAID) {
                add(prepaid, amount);
            }
            if (row.getBookingStatus() == BookingStatus.PENDING) {
                add(awaiting, amount);
            }
        }
        RevenueShareDTO tourTotal = slice(TOUR);
        for (TourSaleRow row : tours) {
            ExpectedRevenueDTO day = byDate.get(row.getTourDate());
            if (day == null) {
                continue;
            }
            day.setTourRevenue(day.getTourRevenue().add(row.tourRevenue()));
            day.setTours(day.getTours() + 1);
            add(tourTotal, row.tourRevenue());
        }
        RevenueShareDTO outstanding = slice("OUTSTANDING");
        unpaidCompleted.forEach(row -> add(outstanding, row.getTotalAmount()));

        return RevenueOutlookResponse.builder()
                .from(from)
                .to(to)
                .totalExpected(booked.getRevenue().add(tourTotal.getRevenue()))
                .bookingRevenue(booked.getRevenue())
                .bookings(booked.getCount())
                .prepaidRevenue(prepaid.getRevenue())
                .prepaidBookings(prepaid.getCount())
                .awaitingConfirmationRevenue(awaiting.getRevenue())
                .awaitingConfirmationBookings(awaiting.getCount())
                .tourRevenue(tourTotal.getRevenue())
                .tours(tourTotal.getCount())
                .byDate(List.copyOf(byDate.values()))
                .overdueRevenue(overdue.getRevenue())
                .overdueBookings(overdue.getCount())
                .outstandingAmount(outstanding.getRevenue())
                .outstandingBookings(outstanding.getCount())
                .build();
    }

    /** NEW then RETURNING: a guest returns when they are among {@code returningCustomerIds}. */
    static List<RevenueShareDTO> byCustomerType(List<CompletedBookingRow> bookings, Set<Long> returningCustomerIds) {
        RevenueShareDTO newGuests = slice(NEW_GUEST);
        RevenueShareDTO returning = slice(RETURNING_GUEST);
        for (CompletedBookingRow row : bookings) {
            add(returningCustomerIds.contains(row.getCustomerId()) ? returning : newGuests, row.getTotalAmount());
        }
        return withShares(List.of(newGuests, returning));
    }

    /** Percentage change, 2 decimals; null when there was nothing before to compare with. */
    static Double growth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        BigDecimal now = current == null ? BigDecimal.ZERO : current;
        return now.subtract(previous)
                .multiply(HUNDRED)
                .divide(previous, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    static double rate(long part, long whole) {
        if (whole <= 0) {
            return 0d;
        }
        return Math.round((part * 10000d) / whole) / 100d;
    }

    /** Same rule as the room-type report: a stay is at least one night. */
    static long nights(BookedRoomNightRow row) {
        return Math.max(1, ChronoUnit.DAYS.between(row.getCheckinDate(), row.getCheckoutDate()));
    }

    static BigDecimal sum(Stream<BigDecimal> values) {
        return values.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static RevenueShareDTO slice(String key) {
        return new RevenueShareDTO(key, 0L, BigDecimal.ZERO, 0d);
    }

    private static void add(RevenueShareDTO slice, BigDecimal amount) {
        slice.setCount(slice.getCount() + 1);
        if (amount != null) {
            slice.setRevenue(slice.getRevenue().add(amount));
        }
    }

    private static List<RevenueShareDTO> withShares(Collection<RevenueShareDTO> slices) {
        BigDecimal total = sum(slices.stream().map(RevenueShareDTO::getRevenue));
        for (RevenueShareDTO slice : slices) {
            slice.setShare(
                    total.signum() == 0
                            ? 0d
                            : slice.getRevenue()
                                    .multiply(HUNDRED)
                                    .divide(total, 2, RoundingMode.HALF_UP)
                                    .doubleValue());
        }
        return List.copyOf(slices);
    }

    /** Whole đồng - VND has no minor unit. */
    private static BigDecimal perUnit(BigDecimal amount, long units) {
        if (units <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(BigDecimal.valueOf(units), 0, RoundingMode.HALF_UP);
    }

    private static double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
