package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Bookings whose payment was recorded in [from, to], by the day it was recorded - the cash-basis view next to the
 * revenue reports (which count a booking on its check-out day). Tour orders have no payment record of their own.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionsResponse {
    LocalDate from;
    LocalDate to;
    BigDecimal total;
    long payments;

    /** CASH / BANK_TRANSFER / VN_PAY with count, amount and share. */
    List<RevenueShareDTO> byMethod;

    /** Every day of the range, empty days included. */
    List<CollectionDayDTO> byDate;
}
