package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

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
public class StaffPerformanceDTO {
    String accountId;
    String username;
    /** From the Employee record when there is one (managers don't have one). */
    String fullName;

    String positionName;
    Long totalBooking;
    BigDecimal totalRevenue;
}
