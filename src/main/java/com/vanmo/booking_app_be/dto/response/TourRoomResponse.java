package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A room of a tour package (or of a tour order), with its price per night. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TourRoomResponse {
    Long roomId;
    String roomNumber;
    String roomTypeName;
    Integer capacity;
    BigDecimal price;
    String pathImage;
}
