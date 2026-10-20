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
}
