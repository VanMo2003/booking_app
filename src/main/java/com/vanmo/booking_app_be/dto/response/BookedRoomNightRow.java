package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * One booked room with its stay window - raw material for occupancy and room-type revenue, both of
 * which need per-night arithmetic that's clearer (and more portable) done in Java than in SQL.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookedRoomNightRow {
    Long roomId;
    Long roomTypeId;
    String roomTypeName;
    BigDecimal roomPrice;
    LocalDate checkinDate;
    LocalDate checkoutDate;
}
