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
public class TopCustomerDTO {
    Long customerId;
    String fullName;
    String phoneNumber;
    /** Completed bookings checking out in the period. */
    Long totalBooking;

    /** Bookings plus tours. */
    BigDecimal totalSpend;

    /** Completed tour orders with tour date in the period... */
    long tours;

    /** ...and their tour amount (already inside totalSpend). */
    BigDecimal tourSpend;

    /** JPQL projection of the bookings; tours are merged in afterwards. */
    public TopCustomerDTO(
            Long customerId, String fullName, String phoneNumber, Long totalBooking, BigDecimal totalSpend) {
        this(customerId, fullName, phoneNumber, totalBooking, totalSpend, 0L, BigDecimal.ZERO);
    }
}
