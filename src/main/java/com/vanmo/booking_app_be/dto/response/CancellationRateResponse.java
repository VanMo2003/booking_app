package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancellationRateResponse {
    LocalDate from;
    LocalDate to;
    long totalBooking;
    long canceledBooking;
    long completedBooking;
    /** Percentage, 0-100, rounded to 2 decimals. */
    double cancellationRate;

    /** What the cancelled bookings were worth. */
    BigDecimal canceledRevenue;

    /** Cancelled bookings the guest had placed in the app... */
    long canceledSelfService;

    /** ...and the ones taken at the front desk. */
    long canceledFrontDesk;

    /** Tour orders with tour date in the period (any status). */
    long tourOrders;

    long tourCanceled;
    long tourCanceledByGuest;
    long tourCanceledByHotel;
    double tourCancellationRate;

    /**
     * Cancelled bookings by how many days before check-in they were cancelled: key SAME_DAY (on the check-in day or
     * later), D1_2, D3_7, D8_30, OVER_30; count, the bookings' value, share of the cancellations (%).
     */
    List<RevenueShareDTO> byLeadTime;

    /** Median days between cancelling and the check-in day; null without cancellations. */
    Long medianCancelLeadDays;

    /** Cancelled two days or less before check-in - the ones a cancellation fee would cover. */
    long lateCancellations;

    double lateCancellationShare;

    /** Who cancelled; bookings cancelled before this was recorded are in neither. */
    long canceledByGuest;

    long canceledByHotel;
}
