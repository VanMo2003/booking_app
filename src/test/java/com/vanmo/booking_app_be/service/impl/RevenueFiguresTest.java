package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.dto.response.BookedRoomNightRow;
import com.vanmo.booking_app_be.dto.response.BranchPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.CompletedBookingRow;
import com.vanmo.booking_app_be.dto.response.OpenBookingRow;
import com.vanmo.booking_app_be.dto.response.PayrollCostRow;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueShareDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.RoomPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;

class RevenueFiguresTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_10 = LocalDate.of(2026, 10, 10);

    private static CompletedBookingRow booking(long amount, PaymentMethod method, String handledBy) {
        return new CompletedBookingRow(1L, BigDecimal.valueOf(amount), method, handledBy, null, null, null);
    }

    /** A completed stay placed {@code daysAhead} days before check-in. */
    private static CompletedBookingRow stay(LocalDate checkin, int nights, int daysAhead) {
        Date placed = Date.from(checkin.minusDays(daysAhead)
                .atTime(9, 30)
                .atZone(ZoneId.systemDefault())
                .toInstant());
        return new CompletedBookingRow(
                1L, BigDecimal.valueOf(1_000_000), PaymentMethod.CASH, null, checkin, checkin.plusDays(nights), placed);
    }

    private static BookedRoomNightRow room(LocalDate checkin, int nights) {
        return new BookedRoomNightRow(1L, 1L, "Deluxe", BigDecimal.valueOf(500_000), checkin, checkin.plusDays(nights));
    }

    private static TourSaleRow tour(long tourAmount, long roomAmount, int guests, boolean ai) {
        return new TourSaleRow(
                7L,
                "Hạ Long",
                guests,
                BigDecimal.valueOf(tourAmount),
                BigDecimal.valueOf(roomAmount),
                BigDecimal.valueOf(tourAmount + roomAmount),
                ai,
                OCT_1);
    }

    private static RevenueFigures.PeriodRows rows(
            LocalDate from,
            LocalDate to,
            List<CompletedBookingRow> bookings,
            long services,
            List<TourSaleRow> tours,
            List<BookedRoomNightRow> soldRooms,
            long occupiedNights) {
        return new RevenueFigures.PeriodRows(
                from,
                to,
                bookings,
                BigDecimal.valueOf(services),
                tours,
                soldRooms,
                10,
                occupiedNights,
                4,
                1,
                BigDecimal.valueOf(650_000),
                BigDecimal.ZERO);
    }

    private static BookedRoomNightRow stayIn(long roomId, LocalDate checkin, int nights) {
        return new BookedRoomNightRow(
                roomId, 1L, "Deluxe", BigDecimal.valueOf(500_000), checkin, checkin.plusDays(nights));
    }

    @Test
    void everyRoomIsListedBusiestFirstWithIdleRoomsKept() {
        List<RevenueFigures.RoomInfo> rooms = List.of(
                new RevenueFigures.RoomInfo(1L, "101", "Deluxe", BigDecimal.valueOf(500_000)),
                new RevenueFigures.RoomInfo(2L, "102", "Deluxe", BigDecimal.valueOf(500_000)),
                new RevenueFigures.RoomInfo(3L, "201", "Suite", BigDecimal.valueOf(900_000)));
        // room 102: a stay from before the period (only 2 of its nights fall in it) and one inside
        List<BookedRoomNightRow> booked = List.of(
                stayIn(1L, OCT_1.plusDays(2), 2), stayIn(2L, OCT_1.minusDays(1), 3), stayIn(2L, OCT_1.plusDays(5), 3));

        List<RoomPerformanceDTO> result =
                RevenueFigures.rooms(rooms, booked, List.of(stayIn(2L, OCT_1.plusDays(5), 3)), OCT_1, OCT_10);

        assertThat(result).extracting(RoomPerformanceDTO::getRoomNumber).containsExactly("102", "101", "201");
        assertThat(result.get(0).getOccupiedNights()).isEqualTo(5);
        assertThat(result.get(0).getOccupancyRate()).isEqualTo(50d);
        assertThat(result.get(0).getCompletedStays()).isEqualTo(1);
        assertThat(result.get(0).getRevenue()).isEqualByComparingTo("1500000");
        assertThat(result.get(2).getOccupiedNights()).isZero();
    }

    @Test
    void payslipsAreSpreadOverTheirMonthAndCountedForTheDaysInThePeriod() {
        List<PayrollCostRow> payslips = List.of(
                // whole September inside the range
                new PayrollCostRow(LocalDate.of(2026, 9, 1), BigDecimal.valueOf(9_000_000)),
                // October: only 1-10 of 31 days are in the range
                new PayrollCostRow(LocalDate.of(2026, 10, 15), BigDecimal.valueOf(3_100_000)));

        BigDecimal cost = RevenueFigures.payrollCost(payslips, LocalDate.of(2026, 9, 1), OCT_10);

        assertThat(cost).isEqualByComparingTo("10000000");
    }

    @Test
    void profitIsRevenueMinusPayrollWithItsMargin() {
        RevenueFigures.PeriodRows withPayroll = new RevenueFigures.PeriodRows(
                OCT_1,
                OCT_10,
                List.of(booking(4_000_000, PaymentMethod.CASH, null)),
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                10,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3_000_000));

        RevenuePeriodDTO period = RevenueFigures.period(withPayroll);

        assertThat(period.getPayrollCost()).isEqualByComparingTo("3000000");
        assertThat(period.getGrossProfit()).isEqualByComparingTo("1000000");
        assertThat(period.getProfitMargin()).isEqualTo(25d);
    }

    @Test
    void profitGrowthNeedsAProfitBefore() {
        RevenueFigures.PeriodRows loss = new RevenueFigures.PeriodRows(
                OCT_1.minusDays(10),
                OCT_1.minusDays(1),
                List.of(booking(1_000_000, PaymentMethod.CASH, null)),
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                10,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3_000_000));

        RevenueSummaryResponse summary = RevenueFigures.summary(
                rows(OCT_1, OCT_10, List.of(booking(2_000_000, PaymentMethod.CASH, null)), 0, List.of(), List.of(), 0),
                loss);

        assertThat(summary.getProfitGrowth()).isNull();
    }

    @Test
    void periodAveragesTheStayAndHowFarAheadGuestsBook() {
        RevenuePeriodDTO period = RevenueFigures.period(rows(
                OCT_1,
                OCT_10,
                // 2 nights booked 10 days ahead, 1 night walk-in, 3 nights booked 5 days ahead
                List.of(stay(OCT_1, 2, 10), stay(OCT_1.plusDays(3), 1, 0), stay(OCT_1.plusDays(5), 3, 5)),
                0,
                List.of(),
                List.of(),
                0));

        assertThat(period.getAverageStayNights()).isEqualTo(2d);
        assertThat(period.getAverageLeadDays()).isEqualTo(5d);
        assertThat(period.getCanceledRevenue()).isEqualByComparingTo("650000");
    }

    @Test
    void periodSplitsRoomsServicesAndToursAndComputesAdrRevpar() {
        RevenuePeriodDTO period = RevenueFigures.period(rows(
                OCT_1,
                OCT_10,
                List.of(
                        booking(1_200_000, PaymentMethod.CASH, null),
                        booking(1_800_000, PaymentMethod.VN_PAY, "staff-5")),
                400_000,
                // the room part of a tour package earns through its own booking, never here
                List.of(tour(900_000, 1_000_000, 3, true)),
                List.of(room(OCT_1, 2), room(OCT_1.plusDays(3), 3)),
                25));

        assertThat(period.getTotalRevenue()).isEqualByComparingTo("3900000");
        assertThat(period.getRoomRevenue()).isEqualByComparingTo("2600000");
        assertThat(period.getServiceRevenue()).isEqualByComparingTo("400000");
        assertThat(period.getTourRevenue()).isEqualByComparingTo("900000");
        assertThat(period.getCompletedBookings()).isEqualTo(2);
        assertThat(period.getCompletedTours()).isEqualTo(1);
        assertThat(period.getTourGuests()).isEqualTo(3);
        assertThat(period.getRoomNightsSold()).isEqualTo(5);
        // 10 rooms x 10 days
        assertThat(period.getAvailableRoomNights()).isEqualTo(100);
        assertThat(period.getOccupancyRate()).isEqualTo(25d);
        assertThat(period.getAdr()).isEqualByComparingTo("520000");
        assertThat(period.getRevpar()).isEqualByComparingTo("26000");
        assertThat(period.getAveragePerBooking()).isEqualByComparingTo("1500000");
        assertThat(period.getCancellationRate()).isEqualTo(25d);
    }

    @Test
    void emptyPeriodHasZeroesNotDivisionErrors() {
        RevenuePeriodDTO period = RevenueFigures.period(new RevenueFigures.PeriodRows(
                OCT_1, OCT_1, List.of(), null, List.of(), List.of(), 0, 0, 0, 0, null, null));

        assertThat(period.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(period.getAdr()).isEqualByComparingTo("0");
        assertThat(period.getRevpar()).isEqualByComparingTo("0");
        assertThat(period.getAveragePerBooking()).isEqualByComparingTo("0");
        assertThat(period.getOccupancyRate()).isZero();
        assertThat(period.getCancellationRate()).isZero();
    }

    @Test
    void oldTourOrdersWithoutTourAmountCountTheirTotal() {
        TourSaleRow legacy = new TourSaleRow(7L, "Sapa", 2, null, null, BigDecimal.valueOf(700_000), false, OCT_1);
        RevenuePeriodDTO period =
                RevenueFigures.period(rows(OCT_1, OCT_10, List.of(), 0, List.of(legacy), List.of(), 0));

        assertThat(period.getTourRevenue()).isEqualByComparingTo("700000");
    }

    @Test
    void summaryComparesWithThePreviousPeriod() {
        RevenueFigures.PeriodRows current = rows(
                OCT_1,
                OCT_10,
                List.of(booking(3_000_000, PaymentMethod.CASH, null)),
                0,
                List.of(),
                List.of(room(OCT_1, 6)),
                30);
        RevenueFigures.PeriodRows previous = rows(
                OCT_1.minusDays(10),
                OCT_1.minusDays(1),
                List.of(booking(2_000_000, PaymentMethod.CASH, null)),
                0,
                List.of(),
                List.of(room(OCT_1.minusDays(10), 4)),
                20);

        RevenueSummaryResponse summary = RevenueFigures.summary(current, previous);

        assertThat(summary.getRevenueGrowth()).isEqualTo(50d);
        assertThat(summary.getBookingGrowth()).isEqualTo(0d);
        // ADR 500k both times, RevPAR 30k vs 20k
        assertThat(summary.getAdrGrowth()).isEqualTo(0d);
        assertThat(summary.getRevparGrowth()).isEqualTo(50d);
        assertThat(summary.getOccupancyChange()).isEqualTo(10d);
    }

    private static RevenueFigures.Period previous(String from, String to) {
        return RevenueFigures.previousPeriod(LocalDate.parse(from), LocalDate.parse(to));
    }

    private static RevenueFigures.Period period(String from, String to) {
        return new RevenueFigures.Period(LocalDate.parse(from), LocalDate.parse(to));
    }

    @Test
    void calendarRangesCompareWithTheSameStretchOneMonthOrYearEarlier() {
        // month to date vs the same days of last month; a whole month vs the whole previous month
        assertThat(previous("2026-10-01", "2026-10-20")).isEqualTo(period("2026-09-01", "2026-09-20"));
        assertThat(previous("2026-09-01", "2026-09-30")).isEqualTo(period("2026-08-01", "2026-08-31"));
        assertThat(previous("2026-03-01", "2026-03-31")).isEqualTo(period("2026-02-01", "2026-02-28"));
        assertThat(previous("2026-03-01", "2026-03-30")).isEqualTo(period("2026-02-01", "2026-02-28"));
        // January to date is still a month: compared with December
        assertThat(previous("2026-01-01", "2026-01-15")).isEqualTo(period("2025-12-01", "2025-12-15"));
        // year to date and whole years vs last year
        assertThat(previous("2026-01-01", "2026-10-20")).isEqualTo(period("2025-01-01", "2025-10-20"));
        assertThat(previous("2026-01-01", "2026-12-31")).isEqualTo(period("2025-01-01", "2025-12-31"));
        assertThat(previous("2025-01-01", "2025-02-28")).isEqualTo(period("2024-01-01", "2024-02-29"));
    }

    @Test
    void otherRangesCompareWithAsManyDaysJustBefore() {
        assertThat(previous("2026-10-14", "2026-10-20")).isEqualTo(period("2026-10-07", "2026-10-13"));
        assertThat(previous("2026-09-15", "2026-10-14")).isEqualTo(period("2026-08-16", "2026-09-14"));
        assertThat(previous("2026-10-10", "2026-10-10")).isEqualTo(period("2026-10-09", "2026-10-09"));
    }

    @Test
    void growthIsLeftOutWhenThePreviousPeriodEarnedNothing() {
        assertThat(RevenueFigures.growth(BigDecimal.TEN, BigDecimal.ZERO)).isNull();
        assertThat(RevenueFigures.growth(BigDecimal.ZERO, BigDecimal.TEN)).isEqualTo(-100d);
    }

    @Test
    void paymentMethodsAlwaysListEveryMethod() {
        List<RevenueShareDTO> slices = RevenueFigures.byPaymentMethod(
                List.of(booking(750_000, PaymentMethod.CASH, null), booking(250_000, PaymentMethod.VN_PAY, null)));

        assertThat(slices).extracting(RevenueShareDTO::getKey).containsExactly("CASH", "BANK_TRANSFER", "VN_PAY");
        assertThat(slices).extracting(RevenueShareDTO::getShare).containsExactly(75d, 0d, 25d);
        assertThat(slices.get(1).getCount()).isZero();
    }

    @Test
    void channelsSplitSelfServiceFrontDeskAndTours() {
        List<RevenueShareDTO> slices = RevenueFigures.byChannel(
                List.of(booking(600_000, PaymentMethod.CASH, null), booking(200_000, PaymentMethod.CASH, "staff-9")),
                List.of(tour(200_000, 500_000, 2, false)));

        assertThat(slices)
                .extracting(RevenueShareDTO::getKey)
                .containsExactly(RevenueFigures.SELF_SERVICE, RevenueFigures.FRONT_DESK, RevenueFigures.TOUR);
        assertThat(slices).extracting(RevenueShareDTO::getShare).containsExactly(60d, 20d, 20d);
    }

    @Test
    void toursAreGroupedPerTourWithAiOrdersCountedAndBiggestFirst() {
        TourSaleRow sapa = new TourSaleRow(
                9L, "Sapa", 4, BigDecimal.valueOf(2_000_000), null, BigDecimal.valueOf(2_000_000), false, OCT_1);
        // a deleted tour keeps its name on the orders
        TourSaleRow deleted = new TourSaleRow(
                null, "Ninh Bình", 1, BigDecimal.valueOf(300_000), null, BigDecimal.valueOf(300_000), false, OCT_1);

        List<RevenueByTourDTO> rows = RevenueFigures.byTour(
                List.of(tour(900_000, 1_000_000, 3, true), sapa, tour(450_000, 0, 1, false), deleted));

        assertThat(rows).extracting(RevenueByTourDTO::getTourName).containsExactly("Sapa", "Hạ Long", "Ninh Bình");
        RevenueByTourDTO haLong = rows.get(1);
        assertThat(haLong.getTourId()).isEqualTo(7L);
        assertThat(haLong.getOrders()).isEqualTo(2);
        assertThat(haLong.getGuests()).isEqualTo(4);
        assertThat(haLong.getTourRevenue()).isEqualByComparingTo("1350000");
        assertThat(haLong.getPackageRoomAmount()).isEqualByComparingTo("1000000");
        assertThat(haLong.getAiOrders()).isEqualTo(1);
        assertThat(rows.get(2).getTourId()).isNull();
    }

    @Test
    void branchesAreRankedByRevenueWithTheirShareAndGrowth() {
        RevenueFigures.PeriodRows emptyBefore =
                rows(OCT_1.minusDays(10), OCT_1.minusDays(1), List.of(), 0, List.of(), List.of(), 0);
        BranchPerformanceDTO small = RevenueFigures.branch(
                6L,
                "Hoàn Kiếm",
                rows(OCT_1, OCT_10, List.of(booking(1_000_000, PaymentMethod.CASH, null)), 0, List.of(), List.of(), 0),
                rows(
                        OCT_1.minusDays(10),
                        OCT_1.minusDays(1),
                        List.of(booking(500_000, PaymentMethod.CASH, null)),
                        0,
                        List.of(),
                        List.of(),
                        0));
        BranchPerformanceDTO big = RevenueFigures.branch(
                7L,
                "Đà Nẵng",
                rows(
                        OCT_1,
                        OCT_10,
                        List.of(booking(2_000_000, PaymentMethod.CASH, null)),
                        0,
                        List.of(tour(1_000_000, 0, 2, false)),
                        List.of(),
                        0),
                emptyBefore);

        List<BranchPerformanceDTO> ranked = RevenueFigures.rankBranches(List.of(small, big));

        assertThat(ranked).extracting(BranchPerformanceDTO::getHotelId).containsExactly(7L, 6L);
        assertThat(ranked).extracting(BranchPerformanceDTO::getRevenueShare).containsExactly(75d, 25d);
        assertThat(ranked.get(1).getRevenueGrowth()).isEqualTo(100d);
        assertThat(ranked.get(1).getPreviousRevenue()).isEqualByComparingTo("500000");
        // nothing earned before: no growth figure rather than an infinite one
        assertThat(ranked.get(0).getRevenueGrowth()).isNull();
    }

    @Test
    void aChainThatEarnedNothingHasZeroShares() {
        RevenueFigures.PeriodRows nothing = rows(OCT_1, OCT_10, List.of(), 0, List.of(), List.of(), 0);
        List<BranchPerformanceDTO> ranked =
                RevenueFigures.rankBranches(List.of(RevenueFigures.branch(6L, "A", nothing, nothing)));

        assertThat(ranked.get(0).getRevenueShare()).isZero();
    }

    private static OpenBookingRow open(long amount, BookingStatus status, PaymentStatus payment, LocalDate checkout) {
        return new OpenBookingRow(1L, BigDecimal.valueOf(amount), status, payment, checkout);
    }

    @Test
    void outlookSpreadsOpenBookingsAndToursOverTheWindowAndFlagsWhatIsLate() {
        LocalDate today = OCT_10;
        LocalDate end = today.plusDays(6);
        TourSaleRow upcomingTour = new TourSaleRow(
                7L,
                "Hạ Long",
                2,
                BigDecimal.valueOf(900_000),
                BigDecimal.valueOf(550_000),
                BigDecimal.valueOf(1_450_000),
                false,
                today.plusDays(2));

        RevenueOutlookResponse outlook = RevenueFigures.outlook(
                today,
                end,
                List.of(
                        open(1_200_000, BookingStatus.CONFIRMED, PaymentStatus.PAID, today),
                        open(800_000, BookingStatus.PENDING, PaymentStatus.UNPAID, today.plusDays(2)),
                        // checked out two days ago on paper, still open
                        open(500_000, BookingStatus.CONFIRMED, PaymentStatus.UNPAID, today.minusDays(2))),
                List.of(upcomingTour),
                List.of(open(300_000, BookingStatus.COMPLETED, PaymentStatus.UNPAID, OCT_1)));

        assertThat(outlook.getByDate()).hasSize(7);
        assertThat(outlook.getByDate().get(0).getBookingRevenue()).isEqualByComparingTo("1200000");
        assertThat(outlook.getByDate().get(2).getBookingRevenue()).isEqualByComparingTo("800000");
        assertThat(outlook.getByDate().get(2).getTourRevenue()).isEqualByComparingTo("900000");
        assertThat(outlook.getBookingRevenue()).isEqualByComparingTo("2000000");
        assertThat(outlook.getBookings()).isEqualTo(2);
        assertThat(outlook.getPrepaidRevenue()).isEqualByComparingTo("1200000");
        assertThat(outlook.getAwaitingConfirmationRevenue()).isEqualByComparingTo("800000");
        assertThat(outlook.getTourRevenue()).isEqualByComparingTo("900000");
        assertThat(outlook.getTotalExpected()).isEqualByComparingTo("2900000");
        assertThat(outlook.getOverdueRevenue()).isEqualByComparingTo("500000");
        assertThat(outlook.getOverdueBookings()).isEqualTo(1);
        assertThat(outlook.getOutstandingAmount()).isEqualByComparingTo("300000");
        assertThat(outlook.getOutstandingBookings()).isEqualTo(1);
    }
}
