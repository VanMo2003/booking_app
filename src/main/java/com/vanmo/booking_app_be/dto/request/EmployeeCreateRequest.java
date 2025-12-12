package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeCreateRequest {
    String pathImage;
    @NotNull String accountId;
    Long hotelId;
    String fullName;
    @Pattern(regexp = "0\\d{9}")
    @Length(min = 10, max = 10)
    @NotNull
    String phoneNumber;
    String gender;
    LocalDate dateOfBirth;
    String hometown;
    BigDecimal salary;
    Long positionId;
}
