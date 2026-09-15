package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A pending or rejected owner corrects the hotel details and sends them back for review. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OwnerResubmitRequest {
    @NotBlank
    String ownerName;

    @NotBlank
    @Email(message = "invalid email")
    String email;

    @NotBlank
    @Pattern(regexp = "0\\d{9}", message = "must match 0xxxxxxxxx")
    String phone;

    @NotBlank
    String hotelName;

    @NotBlank
    String address;

    String description;
}
