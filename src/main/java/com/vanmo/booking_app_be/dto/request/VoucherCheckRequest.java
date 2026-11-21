package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** "What would this code take off this order?" - asked before the guest places it. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherCheckRequest {
    @NotBlank
    String code;

    @NotNull
    Long tourId;

    @NotNull
    @Min(1)
    Integer guests;

    @Min(0)
    Integer children;
}
