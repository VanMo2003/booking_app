package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One branch of a chain over the coming days: how full it is already and what it expects to earn. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BranchOutlookDTO {
    Long hotelId;
    String hotelName;
    long totalRooms;
    long bookedRoomNights;
    long availableRoomNights;

    /** Percentage of the window's room-nights already booked (see {@link RevenueOutlookResponse}). */
    double occupancyOnTheBooks;

    /** The same nights a year earlier; null when nothing was booked then. */
    Double lastYearOccupancy;

    /** Open bookings + open tour orders of the window. */
    BigDecimal totalExpected;
}
