package com.vanmo.booking_app_be.dto.request;

import java.time.LocalDate;

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
}
