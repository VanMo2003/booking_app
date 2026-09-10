package com.vanmo.booking_app_be.dto.request;

import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Room;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingRoomCreateRequest {
    Booking booking;
    Room room;
}
