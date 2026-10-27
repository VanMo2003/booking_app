package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Revenue KPIs of one period, both ends inclusive. Rooms and services earn on the check-out day of a completed
 * booking (like every other revenue report), a tour on its tour date once the order is completed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenuePeriodDTO {
    LocalDate from;
    LocalDate to;

    /** Completed bookings + completed tours. */
    BigDecimal totalRevenue;

    /** Booking totals minus their service lines. */
    BigDecimal roomRevenue;

    BigDecimal serviceRevenue;
    BigDecimal tourRevenue;

    long completedBookings;
    long completedTours;
    long tourGuests;

    /** Nights of the rooms in completed bookings. */
    long roomNightsSold;

    /** Rooms x days of the period. */
    long availableRoomNights;

    /** Same figure as the occupancy report for the period (every booking that was not cancelled). */
    double occupancyRate;

    /** Average daily rate: room revenue per room night sold. */
    BigDecimal adr;

    /** Revenue per available room night: room revenue / available room nights. */
    BigDecimal revpar;

    /** (room + service revenue) per completed booking. */
    BigDecimal averagePerBooking;

    /** Bookings checking in during the period, the base of the cancellation rate. */
    long totalBookings;

    long canceledBookings;
    double cancellationRate;

    /** What the cancelled bookings of the period were worth. */
    BigDecimal canceledRevenue;

    /** Average length of stay of the completed bookings, in nights (1 decimal). */
    double averageStayNights;

    /** Average days between placing a completed booking and checking in (1 decimal). */
    double averageLeadDays;
}
