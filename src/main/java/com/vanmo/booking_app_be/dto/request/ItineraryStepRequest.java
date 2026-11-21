package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One stop of a tour's programme; also how it is sent back. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItineraryStepRequest {
    @Size(max = 60)
    String time;

    @NotBlank
    @Size(max = 255)
    String title;

    @Size(max = 2000)
    String description;
}
