package com.vanmo.booking_app_be.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.vanmo.booking_app_be.entity.enums.TravelTheme;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterestsRequest {
    /** Replaces the guest's interests; empty clears them. */
    @NotNull
    Set<TravelTheme> interests;
}
