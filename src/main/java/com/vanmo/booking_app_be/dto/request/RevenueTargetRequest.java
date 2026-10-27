package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Sets a branch's revenue goal for a month; an amount of 0 removes it. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueTargetRequest {
    @NotNull
    Long hotelId;

    /** "2026-10". */
    @NotNull
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "month phải có dạng yyyy-MM")
    String month;

    @NotNull
    @DecimalMin(value = "0", message = "Mục tiêu không được âm")
    BigDecimal amount;
}
