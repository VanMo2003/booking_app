package com.vanmo.booking_app_be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** How many rooms of one type the branches have, and their average list price. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeStockRow {
    Long roomTypeId;
    String roomTypeName;
    Long rooms;
    Double averagePrice;
}
