package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.vanmo.booking_app_be.dto.response.BookedRoomNightRow;
import com.vanmo.booking_app_be.dto.response.BranchPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.CompletedBookingRow;
import com.vanmo.booking_app_be.dto.response.ExpectedRevenueDTO;
import com.vanmo.booking_app_be.dto.response.OpenBookingRow;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueShareDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;

/** The revenue summary's arithmetic, kept apart from the queries so it can be checked on plain rows. */
final class RevenueFigures {

    static final String SELF_SERVICE = "SELF_SERVICE";
    static final String FRONT_DESK = "FRONT_DESK";
    static final String TOUR = "TOUR";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private RevenueFigures() {}

    /**
     * What one period of the summary is computed from.
     *
     * @param soldRooms rooms of the completed bookings (check-out in the period)
     * @param occupiedRoomNights nights of every booking that was not cancelled, cut to the period
     * @param totalBookings bookings checking in during the period
     * @param canceledRevenue what the cancelled ones among them were worth
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
            BigDecimal canceledRevenue) {

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

        long nightsSold =
                rows.soldRooms().stream().mapToLong(RevenueFigures::nights).sum();
        long availableNights = rows.totalRooms() * rows.days();
        long completed = rows.bookings().size();

        return RevenuePeriodDTO.builder()
                .from(rows.from())
                .to(rows.to())
                .totalRevenue(bookingRevenue.add(tourRevenue))
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
