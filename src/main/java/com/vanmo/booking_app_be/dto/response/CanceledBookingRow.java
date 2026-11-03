package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import com.vanmo.booking_app_be.entity.enums.ChatSide;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A cancelled booking with when it was cancelled, for the cancellation lead-time report. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CanceledBookingRow {
    Long bookingId;
    BigDecimal totalAmount;
    LocalDate checkinDate;

    /** Null for bookings cancelled before the time was recorded; {@link #onUpdate} stands in for it. */
    Date canceledAt;

    Date onUpdate;
    ChatSide canceledBy;
}
