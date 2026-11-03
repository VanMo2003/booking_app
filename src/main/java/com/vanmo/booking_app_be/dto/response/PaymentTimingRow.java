package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.vanmo.booking_app_be.entity.enums.PaymentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A completed booking with when its payment was recorded, for the payment-timing split. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentTimingRow {
    Long bookingId;
    BigDecimal totalAmount;
    PaymentStatus paymentStatus;
    LocalDateTime paidAt;
    LocalDate checkinDate;
    LocalDate checkoutDate;
}
