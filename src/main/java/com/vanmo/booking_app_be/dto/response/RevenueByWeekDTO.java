package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueByWeekDTO {
    Integer year;
    Integer week;
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal totalRevenue;
    Long totalBooking;
}
