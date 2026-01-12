package com.vanmo.booking_app_be.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {
    boolean authenticated;
    String accessToken;
    String refreshToken;
    Role role;
    String accountId;
    ListHotelResponse hotel;
    CustomerResponse customer;
    EmployeeResponse employee;
}
