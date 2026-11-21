package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Guests from one hometown over [from, to]: how many, what they booked and spent. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuestOriginDTO {
    /** As the guests typed it (first spelling seen); null for guests who did not fill it in. */
    String hometown;

    /** Distinct guests. */
    long guests;

    long bookings;
    long tours;

    /** Completed bookings (by check-out) + completed tours (by tour date, tour part only). */
    BigDecimal revenue;

    /** Percentage of the period's revenue across all hometowns. */
    double share;
}
