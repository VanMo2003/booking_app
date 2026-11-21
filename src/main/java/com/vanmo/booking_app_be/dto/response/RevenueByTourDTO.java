package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Completed orders of one tour. {@link #tourRevenue} is what the tour itself earned; the package rooms are a booking of
 * their own and earn at check-out, so {@link #packageRoomAmount} is context only.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueByTourDTO {
    /** Null once the tour itself was deleted; the orders keep its name. */
    Long tourId;

    String tourName;
    long orders;
    long guests;
    BigDecimal tourRevenue;
    BigDecimal packageRoomAmount;

    /** Orders the AI receptionist placed after the guest agreed in chat. */
    long aiOrders;

    /** Tour dates with a completed order. */
    long departures;

    /** departures x the tour's seats per departure; 0 once the tour was deleted or has no limit. */
    long seatsOffered;

    /** guests over seatsOffered, percentage. */
    double fillRate;

    public RevenueByTourDTO(
            Long tourId,
            String tourName,
            long orders,
            long guests,
            BigDecimal tourRevenue,
            BigDecimal packageRoomAmount,
            long aiOrders) {
        this(tourId, tourName, orders, guests, tourRevenue, packageRoomAmount, aiOrders, 0, 0, 0d);
    }
}
