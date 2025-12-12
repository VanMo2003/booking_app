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
public class HotelUpdateRequest {
    @NotNull String name;
    @NotNull String address;
    @NotNull String phone;
    String description;
    @NotNull String category;
    String pathImage;
    boolean active;
}
