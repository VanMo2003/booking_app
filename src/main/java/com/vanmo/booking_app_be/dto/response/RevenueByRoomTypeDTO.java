package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueByRoomTypeDTO {
    Long roomTypeId;
    String roomTypeName;
    /** Rooms booked of this type (one per booked room, not per night). */
    long bookedRooms;

    /** Nights of the completed stays (checking out in the period), whole stays. */
    long roomNights;

    /** What those stays earned (the booking's room part shared among its rooms, see RevenueFigures.roomRevenue). */
    BigDecimal totalRevenue;

    /** Rooms of this type the branches have now; 0 for a type no room has any more. */
    long rooms;

    long availableRoomNights;

    /** Nights in the period with a booking (not cancelled), as in the occupancy report. */
    long occupiedRoomNights;

    double occupancyRate;

    /** totalRevenue per night sold - what a night really went for. */
    BigDecimal adr;

    /** totalRevenue per available room-night. */
    BigDecimal revpar;

    /** Average list price of the type's rooms, to set the ADR against. */
    BigDecimal listPrice;
}
