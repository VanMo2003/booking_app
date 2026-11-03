package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
import com.vanmo.booking_app_be.dto.response.CanceledBookingRow;
import com.vanmo.booking_app_be.dto.response.CollectionDayDTO;
import com.vanmo.booking_app_be.dto.response.CollectionRow;
import com.vanmo.booking_app_be.dto.response.CollectionsResponse;
import com.vanmo.booking_app_be.dto.response.CompletedBookingRow;
import com.vanmo.booking_app_be.dto.response.CustomerTourRow;
import com.vanmo.booking_app_be.dto.response.ExpectedRevenueDTO;
import com.vanmo.booking_app_be.dto.response.OpenBookingRow;
import com.vanmo.booking_app_be.dto.response.PaymentTimingRow;
import com.vanmo.booking_app_be.dto.response.PayrollCostRow;
import com.vanmo.booking_app_be.dto.response.RevenueByDateDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByRoomTypeDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueShareDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.RoomPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.RoomTypeStockRow;
import com.vanmo.booking_app_be.dto.response.StaffPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.StaffTourRow;
import com.vanmo.booking_app_be.dto.response.TargetProgressResponse;
import com.vanmo.booking_app_be.dto.response.TopCustomerDTO;
import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.dto.response.TourStatusCountRow;
import com.vanmo.booking_app_be.dto.response.TrendBucketDTO;
import com.vanmo.booking_app_be.dto.response.WeekdayStatsDTO;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;

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
            BigDecimal payrollCost,
            long bookingsWithServices) {

        /** Without the service attach count (0), as most tests build it. */
        PeriodRows(
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
            this(
                    from,
                    to,
                    bookings,
                    serviceRevenue,
                    tours,
                    soldRooms,
                    totalRooms,
                    occupiedRoomNights,
                    totalBookings,
                    canceledBookings,
                    canceledRevenue,
                    payrollCost,
                    0);
        }

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
                .uniqueGuests(guests(rows.bookings()))
                .revenuePerGuest(perUnit(bookingRevenue, guests(rows.bookings())))
                .serviceAttachRate(rate(rows.bookingsWithServices(), completed))
                .servicePerBooking(perUnit(serviceRevenue, completed))
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
    /**
     * Fills in the rooms already booked for each night of an outlook and the occupancy that makes over the window -
     * the hotel's "on the books" figure. A room counts once a night however many bookings overlap it.
     */
    static void occupancyOnTheBooks(
            RevenueOutlookResponse outlook,
            long totalRooms,
            List<BookedRoomNightRow> booked,
            List<BookedRoomNightRow> bookedLastYear) {
        long bookedNights = 0;
        for (ExpectedRevenueDTO day : outlook.getByDate()) {
            long rooms = roomsBooked(day.getDate(), booked);
            day.setBookedRooms(rooms);
            day.setOccupancyRate(rate(rooms, totalRooms));
            bookedNights += rooms;
        }
        long available = totalRooms * outlook.getByDate().size();
        outlook.setTotalRooms(totalRooms);
        outlook.setBookedRoomNights(bookedNights);
        outlook.setAvailableRoomNights(available);
        outlook.setOccupancyOnTheBooks(rate(bookedNights, available));

        LocalDate from = outlook.getFrom().minusYears(1);
        LocalDate to = outlook.getTo().minusYears(1);
        long lastYearNights = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            lastYearNights += roomsBooked(day, bookedLastYear);
        }
        outlook.setLastYearBookedRoomNights(lastYearNights);
        outlook.setLastYearOccupancy(
                lastYearNights == 0
                        ? null
                        : rate(lastYearNights, totalRooms * (ChronoUnit.DAYS.between(from, to) + 1)));
    }

    /** Rooms taken the night starting on that day; a room counts once however many bookings overlap it. */
    private static long roomsBooked(LocalDate night, List<BookedRoomNightRow> booked) {
        return booked.stream()
                .filter(r -> !night.isBefore(r.getCheckinDate()) && night.isBefore(r.getCheckoutDate()))
                .map(BookedRoomNightRow::getRoomId)
                .distinct()
                .count();
    }

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

    /**
     * What each completed room stay really earned: its booking's room part (total minus its service lines), shared
     * among the booking's rooms by price x nights - so a room whose price changed since keeps what it was sold at. A row
     * without its booking falls back to price x nights. The last room of a booking takes the rounding rest.
     */
    static Map<BookedRoomNightRow, BigDecimal> roomRevenue(
            List<BookedRoomNightRow> rows, Map<Long, BigDecimal> servicesByBooking) {
        Map<BookedRoomNightRow, BigDecimal> earned = new IdentityHashMap<>();
        Map<Long, List<BookedRoomNightRow>> byBooking = new LinkedHashMap<>();
        for (BookedRoomNightRow row : rows) {
            if (row.getBookingId() == null || row.getBookingTotal() == null) {
                earned.put(row, listPrice(row));
            } else {
                byBooking
                        .computeIfAbsent(row.getBookingId(), id -> new ArrayList<>())
                        .add(row);
            }
        }
        byBooking.forEach((bookingId, rooms) -> {
            BigDecimal roomPart = rooms.get(0)
                    .getBookingTotal()
                    .subtract(servicesByBooking.getOrDefault(bookingId, BigDecimal.ZERO))
                    .max(BigDecimal.ZERO);
            BigDecimal weights = sum(rooms.stream().map(RevenueFigures::listPrice));
            BigDecimal allocated = BigDecimal.ZERO;
            for (int i = 0; i < rooms.size(); i++) {
                BigDecimal share;
                if (i == rooms.size() - 1) {
                    share = roomPart.subtract(allocated);
                } else if (weights.signum() == 0) {
                    share = roomPart.divide(BigDecimal.valueOf(rooms.size()), 0, RoundingMode.HALF_UP);
                } else {
                    share = roomPart.multiply(listPrice(rooms.get(i))).divide(weights, 0, RoundingMode.HALF_UP);
                }
                allocated = allocated.add(share);
                earned.put(rooms.get(i), share);
            }
        });
        return earned;
    }

    /** Today's price x nights of the stay. */
    private static BigDecimal listPrice(BookedRoomNightRow row) {
        return row.getRoomPrice() == null
                ? BigDecimal.ZERO
                : row.getRoomPrice().multiply(BigDecimal.valueOf(nights(row)));
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
            Map<Long, BigDecimal> servicesByBooking,
            LocalDate from,
            LocalDate to) {
        Map<BookedRoomNightRow, BigDecimal> earned = roomRevenue(completed, servicesByBooking);
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
            if (room != null) {
                room.setCompletedStays(room.getCompletedStays() + 1);
                room.setRevenue(room.getRevenue().add(earned.getOrDefault(row, BigDecimal.ZERO)));
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
        return byTour(tours, Map.of());
    }

    /** {@link #byTour(List)} plus how full each tour's departures ran, from its seats per departure. */
    static List<RevenueByTourDTO> byTour(List<TourSaleRow> tours, Map<Long, Integer> seatsPerDeparture) {
        Map<String, RevenueByTourDTO> byKey = new LinkedHashMap<>();
        Map<String, Set<LocalDate>> dates = new LinkedHashMap<>();
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
            if (row.getTourDate() != null) {
                dates.computeIfAbsent(key, k -> new HashSet<>()).add(row.getTourDate());
            }
        }
        for (Map.Entry<String, RevenueByTourDTO> entry : byKey.entrySet()) {
            RevenueByTourDTO tour = entry.getValue();
            long departures = dates.getOrDefault(entry.getKey(), Set.of()).size();
            Integer seats = tour.getTourId() == null ? null : seatsPerDeparture.get(tour.getTourId());
            tour.setDepartures(departures);
            tour.setSeatsOffered(seats == null || seats <= 0 ? 0 : departures * seats);
            tour.setFillRate(rate(tour.getGuests(), tour.getSeatsOffered()));
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
            byDate.put(day, new ExpectedRevenueDTO(day, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L, 0d));
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

    /**
     * One branch's month against its goal.
     *
     * @param revenue the month's total revenue so far
     * @param onTheBooks open bookings and tour orders due in the rest of the month
     */
    static TargetProgressResponse targetProgress(
            Long hotelId,
            String hotelName,
            YearMonth month,
            BigDecimal target,
            BigDecimal revenue,
            BigDecimal onTheBooks,
            LocalDate today) {
        LocalDate start = month.atDay(1);
        long elapsed =
                today.isBefore(start) ? 0 : Math.min(month.lengthOfMonth(), ChronoUnit.DAYS.between(start, today) + 1);
        BigDecimal projected = revenue.add(onTheBooks);
        boolean hasTarget = target != null && target.signum() > 0;
        return TargetProgressResponse.builder()
                .hotelId(hotelId)
                .hotelName(hotelName)
                .month(month.toString())
                .target(hasTarget ? target : null)
                .revenue(revenue)
                .projectedRevenue(projected)
                .achievedRate(hasTarget ? percentOf(revenue, target) : null)
                .projectedRate(hasTarget ? percentOf(projected, target) : null)
                .daysElapsed((int) elapsed)
                .daysInMonth(month.lengthOfMonth())
                .build();
    }

    /**
     * The chain's month: revenue and projection of every branch, measured against the sum of the goals that were set -
     * the rates only count the branches that have a goal, so a branch without one cannot inflate them.
     */
    static TargetProgressResponse chainTarget(List<TargetProgressResponse> branches, YearMonth month, LocalDate today) {
        BigDecimal revenue = sum(branches.stream().map(TargetProgressResponse::getRevenue));
        BigDecimal projected = sum(branches.stream().map(TargetProgressResponse::getProjectedRevenue));
        TargetProgressResponse chain =
                targetProgress(null, null, month, null, revenue, projected.subtract(revenue), today);
        List<TargetProgressResponse> targeted =
                branches.stream().filter(b -> b.getTarget() != null).toList();
        if (!targeted.isEmpty()) {
            BigDecimal target = sum(targeted.stream().map(TargetProgressResponse::getTarget));
            chain.setTarget(target);
            chain.setAchievedRate(percentOf(sum(targeted.stream().map(TargetProgressResponse::getRevenue)), target));
            chain.setProjectedRate(
                    percentOf(sum(targeted.stream().map(TargetProgressResponse::getProjectedRevenue)), target));
        }
        return chain;
    }

    /**
     * Each team member's period: the bookings they took (already counted by the query), the ones of theirs that
     * completed and the tours they handled - someone can appear through any of the three. Biggest earner (completed +
     * tours) first. Names are filled in by the caller.
     */
    static List<StaffPerformanceDTO> staff(
            List<StaffPerformanceDTO> taken, List<CompletedBookingRow> completed, List<StaffTourRow> tours) {
        Map<String, StaffPerformanceDTO> byAccount = new LinkedHashMap<>();
        taken.forEach(row -> byAccount.put(row.getAccountId(), row));
        for (CompletedBookingRow sale : completed) {
            if (sale.getHandledByAccountId() == null) {
                continue;
            }
            StaffPerformanceDTO row = byAccount.computeIfAbsent(
                    sale.getHandledByAccountId(),
                    id -> new StaffPerformanceDTO(id, null, null, null, 0L, BigDecimal.ZERO));
            row.setCompletedBookings(row.getCompletedBookings() + 1);
            if (sale.getTotalAmount() != null) {
                row.setCompletedRevenue(row.getCompletedRevenue().add(sale.getTotalAmount()));
            }
        }
        for (StaffTourRow tour : tours) {
            StaffPerformanceDTO row = byAccount.computeIfAbsent(
                    tour.getAccountId(),
                    id -> new StaffPerformanceDTO(id, tour.getUsername(), null, null, 0L, BigDecimal.ZERO));
            row.setToursHandled(tour.getTours() == null ? 0 : tour.getTours());
            row.setTourRevenue(tour.getTourRevenue() == null ? BigDecimal.ZERO : tour.getTourRevenue());
        }
        return byAccount.values().stream()
                .sorted(Comparator.comparing((StaffPerformanceDTO row) ->
                                row.getCompletedRevenue().add(row.getTourRevenue()))
                        .thenComparing(row -> row.getTotalBooking() == null ? 0L : row.getTotalBooking())
                        .reversed())
                .toList();
    }

    /**
     * Guests ranked by what they spent - completed bookings plus completed tours - keeping the top {@code limit}; a
     * guest who only took tours is ranked too.
     */
    static List<TopCustomerDTO> topCustomers(List<TopCustomerDTO> bookings, List<CustomerTourRow> tours, int limit) {
        Map<Long, TopCustomerDTO> byCustomer = new LinkedHashMap<>();
        bookings.forEach(row -> byCustomer.put(row.getCustomerId(), row));
        for (CustomerTourRow tour : tours) {
            TopCustomerDTO row = byCustomer.computeIfAbsent(
                    tour.getCustomerId(),
                    id -> new TopCustomerDTO(id, tour.getFullName(), tour.getPhoneNumber(), 0L, BigDecimal.ZERO));
            BigDecimal spend = tour.getTourSpend() == null ? BigDecimal.ZERO : tour.getTourSpend();
            row.setTours(tour.getTours() == null ? 0 : tour.getTours());
            row.setTourSpend(spend);
            row.setTotalSpend((row.getTotalSpend() == null ? BigDecimal.ZERO : row.getTotalSpend()).add(spend));
        }
        return byCustomer.values().stream()
                .sorted(Comparator.comparing((TopCustomerDTO row) ->
                                row.getTotalSpend() == null ? BigDecimal.ZERO : row.getTotalSpend())
                        .reversed())
                .limit(limit)
                .toList();
    }

    /** Tour orders of a period: all of them, the cancelled ones and who cancelled. */
    record TourCancellations(long orders, long canceled, long byGuest, long byHotel) {
        double rate() {
            return RevenueFigures.rate(canceled, orders);
        }
    }

    static TourCancellations tourCancellations(List<TourStatusCountRow> rows) {
        long orders = 0;
        long canceled = 0;
        long byGuest = 0;
        long byHotel = 0;
        for (TourStatusCountRow row : rows) {
            long count = row.getCount() == null ? 0 : row.getCount();
            orders += count;
            if (row.getStatus() == TourBookingStatus.CANCELED) {
                canceled += count;
                if (row.getCanceledBy() == ChatSide.CUSTOMER) {
                    byGuest += count;
                } else if (row.getCanceledBy() == ChatSide.HOTEL) {
                    byHotel += count;
                }
            }
        }
        return new TourCancellations(orders, canceled, byGuest, byHotel);
    }

    /** Size of one bar of the revenue trend. */
    enum TrendGranularity {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    /**
     * The revenue trend over [from, to]: one bucket per day, ISO week (Monday start), month or year, every bucket
     * present even when empty, the first and last cut to the range.
     *
     * @param bookingsByDay completed bookings by check-out day
     * @param tours completed tour orders (tour date in the range)
     */
    static List<TrendBucketDTO> trend(
            LocalDate from,
            LocalDate to,
            TrendGranularity granularity,
            List<RevenueByDateDTO> bookingsByDay,
            List<TourSaleRow> tours) {
        Map<LocalDate, TrendBucketDTO> buckets = new LinkedHashMap<>();
        for (LocalDate start = bucketStart(from, granularity); !start.isAfter(to); start = next(start, granularity)) {
            LocalDate end = next(start, granularity).minusDays(1);
            buckets.put(
                    start,
                    new TrendBucketDTO(
                            start.isBefore(from) ? from : start,
                            end.isAfter(to) ? to : end,
                            granularity == TrendGranularity.WEEK ? start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) : null,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            0L,
                            0L));
        }
        for (RevenueByDateDTO day : bookingsByDay) {
            TrendBucketDTO bucket = buckets.get(bucketStart(day.getDate(), granularity));
            if (bucket != null && day.getTotalRevenue() != null) {
                bucket.setBookingRevenue(bucket.getBookingRevenue().add(day.getTotalRevenue()));
                bucket.setBookings(bucket.getBookings() + (day.getTotalBooking() == null ? 0 : day.getTotalBooking()));
            }
        }
        for (TourSaleRow tour : tours) {
            TrendBucketDTO bucket = buckets.get(bucketStart(tour.getTourDate(), granularity));
            if (bucket != null) {
                bucket.setTourRevenue(bucket.getTourRevenue().add(tour.tourRevenue()));
                bucket.setTours(bucket.getTours() + 1);
            }
        }
        buckets.values().forEach(b -> b.setTotalRevenue(b.getBookingRevenue().add(b.getTourRevenue())));
        return List.copyOf(buckets.values());
    }

    /** Payments recorded in [from, to] per day and per method (every day listed); no method counts as cash. */
    static CollectionsResponse collections(LocalDate from, LocalDate to, List<CollectionRow> rows) {
        Map<LocalDate, CollectionDayDTO> byDate = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDate.put(
                    day,
                    new CollectionDayDTO(day, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L));
        }
        Map<PaymentMethod, RevenueShareDTO> byMethod = new LinkedHashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            byMethod.put(method, slice(method.name()));
        }
        BigDecimal total = BigDecimal.ZERO;
        long payments = 0;
        for (CollectionRow row : rows) {
            CollectionDayDTO day =
                    row.getPaidAt() == null ? null : byDate.get(row.getPaidAt().toLocalDate());
            if (day == null) {
                continue;
            }
            BigDecimal amount = row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount();
            PaymentMethod method = row.getPaymentMethod() == null ? PaymentMethod.CASH : row.getPaymentMethod();
            switch (method) {
                case BANK_TRANSFER -> day.setBankTransfer(day.getBankTransfer().add(amount));
                case VN_PAY -> day.setVnPay(day.getVnPay().add(amount));
                default -> day.setCash(day.getCash().add(amount));
            }
            day.setTotal(day.getTotal().add(amount));
            day.setPayments(day.getPayments() + 1);
            add(byMethod.get(method), amount);
            total = total.add(amount);
            payments++;
        }
        for (RevenueShareDTO slice : byMethod.values()) {
            slice.setShare(
                    total.signum() == 0
                            ? 0d
                            : slice.getRevenue()
                                    .multiply(HUNDRED)
                                    .divide(total, 2, RoundingMode.HALF_UP)
                                    .doubleValue());
        }
        return CollectionsResponse.builder()
                .from(from)
                .to(to)
                .total(total)
                .payments(payments)
                .byMethod(List.copyOf(byMethod.values()))
                .byDate(List.copyOf(byDate.values()))
                .build();
    }

    /** When completed bookings were paid, and how long the late ones took. */
    record PaymentTiming(List<RevenueShareDTO> byTiming, Double averageDaysToCollect) {}

    static PaymentTiming paymentTiming(List<PaymentTimingRow> rows) {
        Map<String, RevenueShareDTO> slices = new LinkedHashMap<>();
        for (String key : List.of("PREPAID", "AT_STAY", "AFTER_CHECKOUT", "UNPAID", "UNKNOWN")) {
            slices.put(key, slice(key));
        }
        long lateDays = 0;
        long late = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentTimingRow row : rows) {
            String key;
            if (row.getPaymentStatus() != PaymentStatus.PAID) {
                key = "UNPAID";
            } else if (row.getPaidAt() == null) {
                key = "UNKNOWN";
            } else {
                LocalDate paid = row.getPaidAt().toLocalDate();
                if (paid.isBefore(row.getCheckinDate())) {
                    key = "PREPAID";
                } else if (!paid.isAfter(row.getCheckoutDate())) {
                    key = "AT_STAY";
                } else {
                    key = "AFTER_CHECKOUT";
                    lateDays += ChronoUnit.DAYS.between(row.getCheckoutDate(), paid);
                    late++;
                }
            }
            add(slices.get(key), row.getTotalAmount());
            total = total.add(row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount());
        }
        for (RevenueShareDTO slice : slices.values()) {
            slice.setShare(
                    total.signum() == 0
                            ? 0d
                            : slice.getRevenue()
                                    .multiply(HUNDRED)
                                    .divide(total, 2, RoundingMode.HALF_UP)
                                    .doubleValue());
        }
        return new PaymentTiming(
                List.copyOf(slices.values()), late == 0 ? null : Math.round(lateDays * 10d / late) / 10d);
    }

    /** Adds each bucket's share of the payslips and what is left of its revenue after them. */
    static List<TrendBucketDTO> withPayroll(List<TrendBucketDTO> buckets, List<PayrollCostRow> payslips) {
        for (TrendBucketDTO bucket : buckets) {
            BigDecimal cost = payrollCost(payslips, bucket.getStart(), bucket.getEnd());
            bucket.setPayrollCost(cost);
            bucket.setGrossProfit(bucket.getTotalRevenue().subtract(cost));
        }
        return buckets;
    }

    private static LocalDate bucketStart(LocalDate date, TrendGranularity granularity) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private static LocalDate next(LocalDate start, TrendGranularity granularity) {
        return switch (granularity) {
            case DAY -> start.plusDays(1);
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
            case YEAR -> start.plusYears(1);
        };
    }

    /** Nothing completed, booked or earned in either week of a digest: not worth a notification. */
    static boolean quiet(RevenueSummaryResponse summary) {
        return idle(summary.getCurrent()) && idle(summary.getPrevious());
    }

    private static boolean idle(RevenuePeriodDTO period) {
        return period.getCompletedBookings() == 0
                && period.getCompletedTours() == 0
                && period.getTotalBookings() == 0
                && period.getTotalRevenue().signum() == 0;
    }

    /** What, if anything, to tell the branch about its goal this morning. */
    enum TargetAlert {
        NONE,
        REACHED,
        AT_RISK
    }

    /** Below this share of the goal, a mid-month projection counts as at risk. */
    static final double AT_RISK_BELOW = 80d;

    /**
     * Reaching the goal is announced once; "at risk" once, and only from the middle of the month, when the projection
     * has enough of the month behind it to mean something. A goal already reached is never at risk.
     */
    static TargetAlert targetAlert(TargetProgressResponse progress, boolean reachedNotified, boolean atRiskNotified) {
        if (progress.getTarget() == null) {
            return TargetAlert.NONE;
        }
        if (progress.getAchievedRate() >= 100) {
            return reachedNotified ? TargetAlert.NONE : TargetAlert.REACHED;
        }
        boolean pastMidMonth = progress.getDaysElapsed() * 2 >= progress.getDaysInMonth();
        if (!atRiskNotified && pastMidMonth && progress.getProjectedRate() < AT_RISK_BELOW) {
            return TargetAlert.AT_RISK;
        }
        return TargetAlert.NONE;
    }

    private static double percentOf(BigDecimal part, BigDecimal whole) {
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP).doubleValue();
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

    /**
     * Each room type over [from, to]: what its completed stays earned and how many nights they had, against how many
     * rooms of the type there are and how many of their nights were booked - occupancy, ADR, RevPAR and the list price
     * beside them. Types with rooms but no sale are listed too (after the others), so an idle type shows up.
     *
     * @param completed completed stays checking out in the period, with what each earned in {@code earned}
     * @param booked stays (not cancelled) overlapping the period, for the occupancy
     */
    static List<RevenueByRoomTypeDTO> roomTypes(
            LocalDate from,
            LocalDate to,
            List<RoomTypeStockRow> stock,
            List<BookedRoomNightRow> completed,
            Map<BookedRoomNightRow, BigDecimal> earned,
            List<BookedRoomNightRow> booked) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        Map<Long, RevenueByRoomTypeDTO> byType = new LinkedHashMap<>();
        for (RoomTypeStockRow row : stock) {
            long rooms = row.getRooms() == null ? 0 : row.getRooms();
            RevenueByRoomTypeDTO type = roomType(byType, row.getRoomTypeId(), row.getRoomTypeName());
            type.setRooms(rooms);
            type.setAvailableRoomNights(rooms * days);
            type.setListPrice(
                    row.getAveragePrice() == null
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(row.getAveragePrice()).setScale(0, RoundingMode.HALF_UP));
        }
        for (BookedRoomNightRow row : completed) {
            RevenueByRoomTypeDTO type = roomType(byType, row.getRoomTypeId(), row.getRoomTypeName());
            type.setBookedRooms(type.getBookedRooms() + 1);
            type.setRoomNights(type.getRoomNights() + nights(row));
            type.setTotalRevenue(type.getTotalRevenue().add(earned.getOrDefault(row, BigDecimal.ZERO)));
        }
        // a room counts once a night, however many bookings overlap it
        Map<Long, Set<String>> occupied = new LinkedHashMap<>();
        LocalDate end = to.plusDays(1);
        for (BookedRoomNightRow row : booked) {
            LocalDate night = row.getCheckinDate().isBefore(from) ? from : row.getCheckinDate();
            LocalDate last = row.getCheckoutDate().isAfter(end) ? end : row.getCheckoutDate();
            for (; night.isBefore(last); night = night.plusDays(1)) {
                occupied.computeIfAbsent(row.getRoomTypeId(), id -> new HashSet<>())
                        .add(row.getRoomId() + "@" + night);
            }
        }
        for (RevenueByRoomTypeDTO type : byType.values()) {
            long nightsBooked =
                    occupied.getOrDefault(type.getRoomTypeId(), Set.of()).size();
            type.setOccupiedRoomNights(nightsBooked);
            type.setOccupancyRate(rate(nightsBooked, type.getAvailableRoomNights()));
            type.setAdr(perUnit(type.getTotalRevenue(), type.getRoomNights()));
            type.setRevpar(perUnit(type.getTotalRevenue(), type.getAvailableRoomNights()));
        }
        return byType.values().stream()
                .sorted(Comparator.comparing(RevenueByRoomTypeDTO::getTotalRevenue)
                        .reversed()
                        .thenComparing(RevenueByRoomTypeDTO::getRooms, Comparator.reverseOrder()))
                .toList();
    }

    private static RevenueByRoomTypeDTO roomType(Map<Long, RevenueByRoomTypeDTO> byType, Long id, String name) {
        return byType.computeIfAbsent(id, key -> RevenueByRoomTypeDTO.builder()
                .roomTypeId(key)
                .roomTypeName(name)
                .totalRevenue(BigDecimal.ZERO)
                .listPrice(BigDecimal.ZERO)
                .build());
    }

    /** How long before check-in bookings were cancelled, and by whom. */
    record CancellationLeadTimes(
            List<RevenueShareDTO> byLeadTime,
            Long medianDays,
            long late,
            double lateShare,
            long byGuest,
            long byHotel) {}

    /** At most this many days before check-in counts as a late cancellation. */
    static final int LATE_CANCEL_DAYS = 2;

    /**
     * Buckets cancelled bookings by the days between the cancellation (its recorded time, else the last update) and
     * the check-in day; a cancellation on that day or after it (a no-show cleared later) is SAME_DAY.
     */
    static CancellationLeadTimes cancellationLeadTimes(List<CanceledBookingRow> rows) {
        Map<String, RevenueShareDTO> buckets = new LinkedHashMap<>();
        for (String key : List.of("SAME_DAY", "D1_2", "D3_7", "D8_30", "OVER_30")) {
            buckets.put(key, slice(key));
        }
        List<Long> leads = new ArrayList<>();
        long late = 0;
        long byGuest = 0;
        long byHotel = 0;
        for (CanceledBookingRow row : rows) {
            Date canceled = row.getCanceledAt() != null ? row.getCanceledAt() : row.getOnUpdate();
            long lead = canceled == null
                    ? 0
                    : Math.max(
                            0,
                            ChronoUnit.DAYS.between(
                                    canceled.toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate(),
                                    row.getCheckinDate()));
            leads.add(lead);
            String key =
                    lead == 0 ? "SAME_DAY" : lead <= 2 ? "D1_2" : lead <= 7 ? "D3_7" : lead <= 30 ? "D8_30" : "OVER_30";
            add(buckets.get(key), row.getTotalAmount());
            if (lead <= LATE_CANCEL_DAYS) {
                late++;
            }
            if (row.getCanceledBy() == ChatSide.CUSTOMER) {
                byGuest++;
            } else if (row.getCanceledBy() == ChatSide.HOTEL) {
                byHotel++;
            }
        }
        buckets.values().forEach(bucket -> bucket.setShare(rate(bucket.getCount(), rows.size())));
        return new CancellationLeadTimes(
                List.copyOf(buckets.values()),
                ChatFigures.median(leads),
                late,
                rate(late, rows.size()),
                byGuest,
                byHotel);
    }

    /** Distinct guests among the bookings (rows without a guest are left out). */
    static long guests(List<CompletedBookingRow> bookings) {
        return bookings.stream()
                .map(CompletedBookingRow::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
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
