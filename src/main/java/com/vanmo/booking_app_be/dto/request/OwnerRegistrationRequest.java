package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Public sign-up of a HOTEL_OWNER: login credentials plus the basic hotel details an admin reviews. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OwnerRegistrationRequest {
    @NotBlank
    @Pattern(regexp = "\\S{6,}", message = "must be at least 6 characters without spaces")
    String username;

    @NotBlank
    @Size(min = 6, message = "must be at least 6 characters")
    String password;

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
