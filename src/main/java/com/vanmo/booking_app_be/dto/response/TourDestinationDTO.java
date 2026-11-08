package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A place guests can search tours for, with how many tours go there and from what price. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourDestinationDTO {
    String destination;
    Long tours;
    BigDecimal fromPrice;

    /** A picture of one of its tours, for the card. */
    String pathImage;
}
