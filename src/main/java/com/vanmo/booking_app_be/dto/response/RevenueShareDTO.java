package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One slice of the period's revenue - a payment method or a sales channel. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueShareDTO {
    /** CASH / BANK_TRANSFER / VN_PAY, or SELF_SERVICE / FRONT_DESK / TOUR. */
    String key;

    long count;
    BigDecimal revenue;

    /** Percentage of the slices' total, 0-100. */
    double share;
}
