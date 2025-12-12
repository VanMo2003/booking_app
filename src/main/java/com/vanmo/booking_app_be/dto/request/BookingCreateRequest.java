package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreateRequest {
    @NotNull LocalDate checkinDate;
    @NotNull LocalDate checkoutDate;
    String bookingStatus; // enum BookingStatus
    String paymentMethod; // enum PaymentMethod
    Long hotelId;
    Long customerId;
    Set<Long> bookingRooms;
    Set<Long> bookingServices;
    BigDecimal totalAmount;
    String note;
}
