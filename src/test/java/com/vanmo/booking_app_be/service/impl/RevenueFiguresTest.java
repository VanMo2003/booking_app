package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.dto.response.BookedRoomNightRow;
import com.vanmo.booking_app_be.dto.response.CompletedBookingRow;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueShareDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;

class RevenueFiguresTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_10 = LocalDate.of(2026, 10, 10);

    private static CompletedBookingRow booking(long amount, PaymentMethod method, String handledBy) {
        return new CompletedBookingRow(1L, BigDecimal.valueOf(amount), method, handledBy);
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
                ai);
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
                from, to, bookings, BigDecimal.valueOf(services), tours, soldRooms, 10, occupiedNights, 4, 1);
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
        RevenuePeriodDTO period = RevenueFigures.period(
                new RevenueFigures.PeriodRows(OCT_1, OCT_1, List.of(), null, List.of(), List.of(), 0, 0, 0, 0));

        assertThat(period.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(period.getAdr()).isEqualByComparingTo("0");
        assertThat(period.getRevpar()).isEqualByComparingTo("0");
        assertThat(period.getAveragePerBooking()).isEqualByComparingTo("0");
        assertThat(period.getOccupancyRate()).isZero();
        assertThat(period.getCancellationRate()).isZero();
    }

    @Test
    void oldTourOrdersWithoutTourAmountCountTheirTotal() {
        TourSaleRow legacy = new TourSaleRow(7L, "Sapa", 2, null, null, BigDecimal.valueOf(700_000), false);
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
                9L, "Sapa", 4, BigDecimal.valueOf(2_000_000), null, BigDecimal.valueOf(2_000_000), false);
        // a deleted tour keeps its name on the orders
        TourSaleRow deleted = new TourSaleRow(
                null, "Ninh Bình", 1, BigDecimal.valueOf(300_000), null, BigDecimal.valueOf(300_000), false);

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
}
