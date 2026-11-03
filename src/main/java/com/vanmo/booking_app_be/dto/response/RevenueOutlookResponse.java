package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Revenue still to come over [from, to] (from = today) and money still to collect. Open = PENDING or CONFIRMED; a
 * booking counts on its check-out day and a tour order on its tour date, the same days the revenue reports use once
 * they complete.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueOutlookResponse {
    LocalDate from;
    LocalDate to;

    /** Open bookings + open tour orders of the window. */
    BigDecimal totalExpected;

    BigDecimal bookingRevenue;
    long bookings;

    /** Part of the bookings the guest already paid (VNPay or marked paid). */
    BigDecimal prepaidRevenue;

    long prepaidBookings;

    /** Part of the bookings the branch has not confirmed yet - the least certain money. */
    BigDecimal awaitingConfirmationRevenue;

    long awaitingConfirmationBookings;

    /** Tour part only; package rooms are bookings and already counted above. */
    BigDecimal tourRevenue;

    long tours;

    /** Every day of the window, empty days included. */
    List<ExpectedRevenueDTO> byDate;

    /** Rooms of the branches, and the room-nights of the window already booked out of all of them. */
    long totalRooms;

    long bookedRoomNights;

    long availableRoomNights;

    /** bookedRoomNights over availableRoomNights, percentage: the occupancy already on the books. */
    double occupancyOnTheBooks;

    /**
     * How full the same nights a year earlier ended up (any booking not cancelled, today's rooms), percentage - what
     * the window can still grow towards. Null when nothing was booked then, e.g. a branch that did not exist yet.
     */
    Double lastYearOccupancy;

    long lastYearBookedRoomNights;

    /** Open bookings whose check-out day already passed - nobody checked the guest out. */
    BigDecimal overdueRevenue;

    long overdueBookings;

    /** Completed bookings whose payment was never recorded as paid. */
    BigDecimal outstandingAmount;

    long outstandingBookings;
}
