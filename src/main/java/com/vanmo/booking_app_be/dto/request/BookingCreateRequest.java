package com.vanmo.booking_app_be.dto.request;

import static com.vanmo.booking_app_be.entity.enums.BookingStatus.PENDING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreateRequest {
    @NotNull
    LocalDate checkinDate;

    @NotNull
    LocalDate checkoutDate;

    String bookingStatus = PENDING.name();
    String paymentMethod;
    //    String paymentStatus;
    Long hotelId;
    Long customerId;
    Set<Long> rooms;
    Set<Long> services;
    BigDecimal totalAmount;
    String note;
}
