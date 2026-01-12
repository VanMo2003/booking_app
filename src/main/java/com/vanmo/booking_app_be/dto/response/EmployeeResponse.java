package com.vanmo.booking_app_be.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeResponse {
    Long id;
    String pathImage;
    String accountId;
    String fullName;
    String phoneNumber;
    String gender;
    LocalDate dateOfBirth;
    String hometown;
    BigDecimal salary;
    String hotelName;
    String positionName;
    Date onCreate;
    Date onUpdate;
}
