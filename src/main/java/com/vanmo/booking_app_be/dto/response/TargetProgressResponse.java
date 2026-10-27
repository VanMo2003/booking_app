package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * A month's revenue against its goal, for one branch (hotelId set) or a whole chain (hotelId left out). Revenue is the
 * month's total so far (completed bookings by check-out day, completed tours by tour date); the projection adds the
 * open bookings and tour orders due in the rest of the month.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TargetProgressResponse {
    Long hotelId;
    String hotelName;

    /** "2026-10". */
    String month;

    /** Left out when no goal was set (for a chain: when none of its branches has one). */
    BigDecimal target;

    BigDecimal revenue;
    BigDecimal projectedRevenue;

    /** revenue / target, percentage; left out without a target. */
    Double achievedRate;

    /** projectedRevenue / target, percentage; left out without a target. */
    Double projectedRate;

    /** Days of the month already started (0 for a future month, all of them for a past one). */
    int daysElapsed;

    int daysInMonth;
}
