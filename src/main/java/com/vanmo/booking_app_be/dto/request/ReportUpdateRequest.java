package com.vanmo.booking_app_be.dto.request;

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
public class ReportUpdateRequest {
    LocalDate reportingMonth;
    BigDecimal totalRevenue;
    Integer totalBooking;
    Integer totalService;
}
