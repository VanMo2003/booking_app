package com.vanmo.booking_app_be.dto.request;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourBookingCreateRequest {
    @NotNull
    Long tourId;

    @NotNull
    LocalDate tourDate;

    @NotNull
    @Min(1)
    Integer guests;

    @Size(max = 500)
    String note;

    /** Rooms of the tour package for the stay; required when the tour comes with rooms. */
    Set<Long> roomIds;
}
