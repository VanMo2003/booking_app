package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One branch of the chain over the chosen period, next to its own previous period. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchPerformanceDTO {
    Long hotelId;
    String hotelName;

    /** Same KPIs as the revenue summary, for this branch only. */
    RevenuePeriodDTO current;

    BigDecimal previousRevenue;

    /** Percentage change versus the branch's previous period; left out when it earned nothing then. */
    Double revenueGrowth;

    /** This branch's part of the chain's revenue in the period, 0-100. */
    double revenueShare;

    /** Guest reviews written in the period and their average stars (null without reviews). */
    long reviews;

    Double averageRating;
}
