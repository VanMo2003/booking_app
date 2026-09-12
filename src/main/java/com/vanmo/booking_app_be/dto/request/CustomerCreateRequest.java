package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerCreateRequest {
    String pathImage;

    /** Optional: omitted for a walk-in guest profile created by staff (no login account). */
    String accountId;

    String fullName;

    @Pattern(regexp = "0\\d{9}")
    @Length(min = 10, max = 10)
    @NotNull
    String phoneNumber;

    String gender;
    String hometown;
}
