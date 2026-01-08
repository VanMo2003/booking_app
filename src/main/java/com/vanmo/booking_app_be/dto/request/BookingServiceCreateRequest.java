package com.vanmo.booking_app_be.dto.request;

import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Service;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingServiceCreateRequest {
    Booking booking;
    Service service;
    Long quantity;
    BigDecimal total;
}
