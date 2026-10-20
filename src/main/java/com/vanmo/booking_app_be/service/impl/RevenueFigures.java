package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.vanmo.booking_app_be.dto.response.BookedRoomNightRow;
import com.vanmo.booking_app_be.dto.response.CompletedBookingRow;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueShareDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;

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
            long canceledBookings) {

        long days() {
            return ChronoUnit.DAYS.between(from, to) + 1;
        }
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
                .build();
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
