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
    /** The room's price today - not necessarily what this stay was sold at. */
    BigDecimal roomPrice;

    LocalDate checkinDate;
    LocalDate checkoutDate;

    /** The booking the room belongs to and its total (rooms + services), for sharing out what it really earned. */
    Long bookingId;

    BigDecimal bookingTotal;

    /** A row without its booking (occupancy maths only needs the room and the dates). */
    public BookedRoomNightRow(
            Long roomId,
            Long roomTypeId,
            String roomTypeName,
            BigDecimal roomPrice,
            LocalDate checkinDate,
            LocalDate checkoutDate) {
        this(roomId, roomTypeId, roomTypeName, roomPrice, checkinDate, checkoutDate, null, null);
    }
}
