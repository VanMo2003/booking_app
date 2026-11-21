package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * One bar of the revenue trend (a day, ISO week, month or year, cut to the requested range): completed bookings by
 * check-out day and completed tours by tour date, the same revenue the summary counts.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrendBucketDTO {
    LocalDate start;
    LocalDate end;

    /** ISO week number, only for weekly buckets. */
    Integer week;

    BigDecimal bookingRevenue;
    BigDecimal tourRevenue;
    BigDecimal totalRevenue;
    long bookings;
    long tours;

    /** Approved and paid payslips spread over the bucket's days (see RevenuePeriodDTO#payrollCost). */
    BigDecimal payrollCost;

    /** totalRevenue - payrollCost; can be negative. */
    BigDecimal grossProfit;

    public TrendBucketDTO(
            LocalDate start,
            LocalDate end,
            Integer week,
            BigDecimal bookingRevenue,
            BigDecimal tourRevenue,
            BigDecimal totalRevenue,
            long bookings,
            long tours) {
        this(start, end, week, bookingRevenue, tourRevenue, totalRevenue, bookings, tours, null, null);
    }
}
