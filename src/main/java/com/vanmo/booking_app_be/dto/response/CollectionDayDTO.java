package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Money recorded as received on one day, by payment method. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionDayDTO {
    LocalDate date;
    BigDecimal cash;
    BigDecimal bankTransfer;
    BigDecimal vnPay;
    BigDecimal total;
    long payments;
}
