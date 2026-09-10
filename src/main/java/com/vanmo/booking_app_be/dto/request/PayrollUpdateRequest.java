package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PayrollUpdateRequest {
    LocalDate month;
    BigDecimal totalSalary;
    Long employeeId;
    String status; // enum PayrollStatus
}
