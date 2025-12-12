package com.vanmo.booking_app_be.dto.request;

import com.vanmo.booking_app_be.entity.Employee;
import com.vanmo.booking_app_be.entity.enums.PayrollStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PayrollCreateRequest {
    LocalDate month;
    BigDecimal totalSalary;
    Long employeeId;
    String status; // enum PayrollStatus
}
