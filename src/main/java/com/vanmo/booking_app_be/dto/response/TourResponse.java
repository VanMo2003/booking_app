package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
public class TourResponse {
    Long id;
    String name;
    String description;
    BigDecimal price;
    String duration;
    String departure;
    String includes;
    Integer maxGuests;
    String pathImage;
    boolean available;
    Integer stayNights;
    /** Rooms the package uses, cheapest first; empty when the tour is sold without a room. */
    List<TourRoomResponse> rooms;

    Long hotelId;
    Date onCreate;
    Date onUpdate;
}
