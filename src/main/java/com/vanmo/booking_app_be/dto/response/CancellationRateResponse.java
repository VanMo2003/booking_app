package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
