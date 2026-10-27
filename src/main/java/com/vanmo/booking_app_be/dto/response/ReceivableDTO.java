package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vanmo.booking_app_be.entity.enums.PaymentMethod;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A completed booking whose payment was never recorded: money the front desk still has to collect or confirm. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReceivableDTO {
    Long bookingId;
    Long hotelId;
    String hotelName;
    String customerName;
    String phoneNumber;
    LocalDate checkoutDate;
    BigDecimal totalAmount;
    PaymentMethod paymentMethod;

    /** Days since check-out (0 on the day itself). */
    long daysOverdue;

    /** JPQL projection; {@link #daysOverdue} is filled in afterwards. */
    public ReceivableDTO(
            Long bookingId,
            Long hotelId,
            String hotelName,
            String customerName,
            String phoneNumber,
            LocalDate checkoutDate,
            BigDecimal totalAmount,
            PaymentMethod paymentMethod) {
        this(bookingId, hotelId, hotelName, customerName, phoneNumber, checkoutDate, totalAmount, paymentMethod, 0L);
    }
}
