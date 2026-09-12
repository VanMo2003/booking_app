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

    long roomNights;
    /** room.price x nights, summed - see ReportServiceImpl for why it's derived this way. */
    BigDecimal totalRevenue;
}
