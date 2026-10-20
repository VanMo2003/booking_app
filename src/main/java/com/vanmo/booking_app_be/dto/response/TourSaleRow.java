package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * One completed tour order. Only {@link #tourAmount} is tour revenue: the package's rooms are a normal booking and
 * earn when the guest checks out, so {@link #roomAmount} is shown for context, never added twice.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourSaleRow {
    Long tourId;
    String tourName;
    Integer guests;
    BigDecimal tourAmount;
    BigDecimal roomAmount;
    BigDecimal totalAmount;
    Boolean createdByAi;

    /** Orders placed before tours had rooms only stored the total. */
    public BigDecimal tourRevenue() {
        if (tourAmount != null) {
            return tourAmount;
        }
        return totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }
}
