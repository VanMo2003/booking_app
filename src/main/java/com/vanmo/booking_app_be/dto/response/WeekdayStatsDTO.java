package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One day of the week (1 = Monday ... 7 = Sunday) averaged over every such day of [from, to]. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeekdayStatsDTO {
    int dayOfWeek;

    /** How many of this weekday the period has. */
    long days;

    /** Rooms occupied (bookings not cancelled) over rooms available on those days, percentage. */
    double averageOccupancy;

    /** Revenue of those days (completed bookings by check-out day, completed tours by tour date) per day. */
    BigDecimal averageRevenue;

    BigDecimal totalRevenue;
}
