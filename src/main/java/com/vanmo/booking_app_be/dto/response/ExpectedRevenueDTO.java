package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Revenue expected on one day: open bookings checking out that day and open tour orders departing that day. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpectedRevenueDTO {
    LocalDate date;
    BigDecimal bookingRevenue;
    BigDecimal tourRevenue;
    long bookings;
    long tours;
}
