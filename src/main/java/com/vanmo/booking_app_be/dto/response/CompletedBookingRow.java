package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import com.vanmo.booking_app_be.entity.enums.PaymentMethod;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * One completed booking as the revenue summary needs it: what it earned, how it was paid and whether the front desk
 * took it (null handledBy = the guest booked it in the app). Summed in Java like {@link BookedRoomNightRow}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompletedBookingRow {
    Long bookingId;
    BigDecimal totalAmount;
    PaymentMethod paymentMethod;
    String handledByAccountId;
    LocalDate checkinDate;
    LocalDate checkoutDate;

    /** When the booking was placed - the start of its lead time. */
    Date createdAt;

    Long customerId;
}
