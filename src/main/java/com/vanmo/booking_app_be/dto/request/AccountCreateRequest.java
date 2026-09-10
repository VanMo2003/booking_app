package com.vanmo.booking_app_be.dto.request;

import static com.vanmo.booking_app_be.entity.enums.Role.CUSTOMER;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountCreateRequest {
    @NotNull
    String username;

    @NotNull
    String password;

    @NotNull
    String role = CUSTOMER.name();

    boolean status = true;
}
