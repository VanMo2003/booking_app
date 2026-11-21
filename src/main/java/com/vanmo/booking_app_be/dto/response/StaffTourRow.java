package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Completed tour orders one team member handled last, with their tour revenue. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffTourRow {
    String accountId;
    String username;
    Long tours;
    BigDecimal tourRevenue;
}
