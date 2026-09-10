package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelChainUpdateRequest {
    @NotBlank
    String name;

    String description;
    String pathImage;
}
