package com.vanmo.booking_app_be.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * The period's KPIs next to the period of the same length just before it. Growth figures are percentages and are left
 * out when the previous period had nothing to compare with.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueSummaryResponse {
    RevenuePeriodDTO current;
    RevenuePeriodDTO previous;

    Double revenueGrowth;
    Double bookingGrowth;
    Double adrGrowth;
    Double revparGrowth;

    /** Change of grossProfit; only when the previous period made a profit. */
    Double profitGrowth;

    /** Occupancy difference in percentage points (current - previous). */
    Double occupancyChange;

    /** Completed bookings by payment method; tours are paid at the hotel and are not in here. */
    List<RevenueShareDTO> byPaymentMethod;

    /** SELF_SERVICE (booked in the app), FRONT_DESK (taken by staff) and TOUR. */
    List<RevenueShareDTO> byChannel;
}
