package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

import com.vanmo.booking_app_be.entity.enums.DiscountType;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Create (all required fields) or partial update (null = keep) of a voucher. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherRequest {
    @Size(min = 3, max = 30)
    @Pattern(regexp = "[A-Za-z0-9_-]+", message = "chỉ gồm chữ, số, - và _")
    String code;

    @Size(max = 255)
    String title;

    @Size(max = 1000)
    String description;

    DiscountType discountType;

    @Positive
    BigDecimal value;

    @PositiveOrZero
    BigDecimal maxDiscount;

    @PositiveOrZero
    BigDecimal minOrderAmount;

    LocalDate startsAt;
    LocalDate endsAt;

    /** 0 = unlimited. */
    @PositiveOrZero
    Integer usageLimit;

    @Min(1)
    @Max(20)
    Integer perCustomerLimit;

    Boolean active;

    /** Set on create: the chain the voucher belongs to. */
    Long hotelChainId;

    /** Only this branch of the chain; null on create = every branch. */
    Long hotelId;
}
