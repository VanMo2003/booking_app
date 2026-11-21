package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One guest's completed tour orders in a period, with their tour amount. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerTourRow {
    Long customerId;
    String fullName;
    String phoneNumber;
    Long tours;
    BigDecimal tourSpend;
}
