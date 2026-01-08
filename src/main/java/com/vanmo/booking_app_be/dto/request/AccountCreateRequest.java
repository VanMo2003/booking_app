package com.vanmo.booking_app_be.dto.request;

import com.vanmo.booking_app_be.entity.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import static com.vanmo.booking_app_be.entity.enums.Role.CUSTOMER;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountCreateRequest {
    @NotNull String username;
    @NotNull String password;
    @NotNull String role = CUSTOMER.name();
    boolean status = true;
}
