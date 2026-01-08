package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static com.vanmo.booking_app_be.entity.enums.BookingStatus.PENDING;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreateRequest {
    @NotNull LocalDate checkinDate;
    @NotNull LocalDate checkoutDate;
    String bookingStatus = PENDING.name();
    String paymentMethod;
    Long hotelId;
    Long customerId;
    Set<Long> rooms;
    Set<Long> services;
    BigDecimal totalAmount;
    String note;
}
