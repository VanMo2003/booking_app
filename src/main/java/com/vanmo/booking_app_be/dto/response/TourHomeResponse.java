package com.vanmo.booking_app_be.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** The guests' home screen: tours to show off, tours on sale, picks for the guest's interests, places to go. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourHomeResponse {
    List<TourResponse> featured;
    List<TourResponse> deals;

    /** Empty when no interests were given. */
    List<TourResponse> forYou;

    List<TourDestinationDTO> destinations;
}
