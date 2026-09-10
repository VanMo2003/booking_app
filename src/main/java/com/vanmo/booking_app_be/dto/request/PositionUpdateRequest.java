package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PositionUpdateRequest {
    @NotNull
    String name;

    String description;
}
