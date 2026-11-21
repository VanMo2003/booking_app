package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** The order's tour amount before and after the code; {@code message} says why a code does not apply. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherCheckResponse {
    boolean valid;
    String message;
    String code;
    BigDecimal tourAmount;
    BigDecimal discount;
    BigDecimal finalAmount;
}
