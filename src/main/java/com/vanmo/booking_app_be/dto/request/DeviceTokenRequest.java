package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceTokenRequest {
    @NotBlank
    @Size(max = 512)
    String token;

    /** android, ios or web. */
    @Size(max = 20)
    String platform;
}
