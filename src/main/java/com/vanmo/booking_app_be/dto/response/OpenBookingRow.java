package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A booking as the revenue outlook needs it: what it is worth, where it stands, when it checks out. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OpenBookingRow {
    Long bookingId;
    BigDecimal totalAmount;
    BookingStatus bookingStatus;
    PaymentStatus paymentStatus;
    LocalDate checkoutDate;
}
