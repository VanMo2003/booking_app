package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Revenue expected on one day: open bookings checking out that day and open tour orders departing that day - plus the
 * rooms already booked for that night.
 */
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

    /** Rooms booked for the night starting that day (any booking not cancelled, as in the occupancy report). */
    long bookedRooms;

    /** bookedRooms over the branch's rooms, percentage. */
    double occupancyRate;
}
