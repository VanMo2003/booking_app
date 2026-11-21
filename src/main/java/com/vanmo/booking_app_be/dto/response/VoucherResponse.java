package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.DiscountType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoucherResponse {
    Long id;
    String code;
    String title;
    String description;
    DiscountType discountType;
    BigDecimal value;
    BigDecimal maxDiscount;
    BigDecimal minOrderAmount;
    LocalDate startsAt;
    LocalDate endsAt;
    Integer usageLimit;
    int usedCount;
    int perCustomerLimit;
    boolean active;
    Long hotelChainId;
    String hotelChainName;
    /** Null = every branch of the chain. */
    Long hotelId;

    String hotelName;
}
