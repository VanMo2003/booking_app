package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Partial update: fields left null keep their value. An empty string clears a text field (or removes the photo);
 * {@code maxGuests: 0} removes the group limit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourUpdateRequest {
    @Size(min = 1, max = 255)
    String name;

    String description;

    @Positive
    BigDecimal price;

    @Size(max = 100)
    String duration;

    @Size(max = 255)
    String departure;

    @Size(max = 1000)
    String includes;

    @Min(0)
    Integer maxGuests;

    @Size(max = 1000)
    String pathImage;

    Boolean available;
}
