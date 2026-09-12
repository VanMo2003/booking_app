package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;

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
public class CancellationRateResponse {
    LocalDate from;
    LocalDate to;
    long totalBooking;
    long canceledBooking;
    long completedBooking;
    /** Percentage, 0-100, rounded to 2 decimals. */
    double cancellationRate;
}
