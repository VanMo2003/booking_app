package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.vanmo.booking_app_be.entity.enums.PaymentMethod;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A booking payment recorded at a time, for the collections ledger. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionRow {
    LocalDateTime paidAt;
    PaymentMethod paymentMethod;
    BigDecimal totalAmount;
}
