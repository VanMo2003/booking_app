package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelCreateRequest {
    @NotBlank String name;
    @NotBlank String address;
    @NotBlank String phone;
    @NotBlank String category;
    String description;
    String pathImage;
    boolean active = true;
    String accountId;
}
