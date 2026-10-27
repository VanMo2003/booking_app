package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One room of a branch over [from, to]: how often it was occupied and what its completed stays earned. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomPerformanceDTO {
    Long roomId;
    String roomNumber;
    String roomTypeName;
    BigDecimal price;

    /** Nights of the period with a booking that was not cancelled. */
    long occupiedNights;

    /** occupiedNights / days of the period, percentage. */
    double occupancyRate;

    /** Completed stays checking out in the period. */
    long completedStays;

    /** room price x nights of those completed stays (same rule as revenue by room type). */
    BigDecimal revenue;
}
