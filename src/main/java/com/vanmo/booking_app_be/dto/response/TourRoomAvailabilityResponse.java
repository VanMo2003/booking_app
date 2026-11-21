package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** The stay that goes with a tour on a given day, and which of the tour's rooms are still free for it. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourRoomAvailabilityResponse {
    Long tourId;
    Long hotelId;
    LocalDate checkin;
    LocalDate checkout;
    int nights;
    /** False when the tour has no rooms attached: it is booked on its own. */
    boolean withRooms;

    List<TourRoomResponse> rooms;
}
