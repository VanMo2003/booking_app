package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One completed booking or tour with its guest's hometown, for the guest-origin report. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuestOriginRow {
    Long customerId;
    String hometown;
    BigDecimal amount;
}
