package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.vanmo.booking_app_be.entity.enums.TravelTheme;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourCreateRequest {
    @NotBlank
    @Size(max = 255)
    String name;

    String description;

    /** Per guest. */
    @NotNull
    @Positive
    BigDecimal price;

    @Size(max = 100)
    String duration;

    @Size(max = 255)
    String departure;

    @Size(max = 1000)
    String includes;

    @Min(1)
    Integer maxGuests;

    @Size(max = 1000)
    String pathImage;

    /** Nights of the stay included with the tour; 1 when left out. */
    @Min(1)
    @Max(30)
    Integer stayNights;

    /** Rooms of the branch the package uses; empty = the tour is sold without a room. */
    Set<Long> roomIds;

    @Builder.Default
    boolean available = true;

    @NotNull
    Long hotelId;

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
