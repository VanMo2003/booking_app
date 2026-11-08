package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.vanmo.booking_app_be.entity.enums.TravelTheme;

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

    @Min(1)
    @Max(30)
    Integer stayNights;

    /** Replaces the package's rooms; null keeps them, empty removes them all. */
    Set<Long> roomIds;

    @Size(max = 255)
    String destination;

    Set<TravelTheme> themes;

    @Valid
    @Size(max = 30)
    List<ItineraryStepRequest> itinerary;

    @PositiveOrZero
    BigDecimal childPrice;

    @Size(max = 2000)
    String cancellationPolicy;

    Boolean featured;

    /** 0 switches the promotion off. */
    @Min(0)
    @Max(90)
    Integer discountPercent;

    LocalDate promotionEndsAt;
}
