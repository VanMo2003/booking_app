package com.vanmo.booking_app_be.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAvailabilityRequest {
    Long hotelId;
    LocalDate checkinDate;
    LocalDate checkoutDate;
}
