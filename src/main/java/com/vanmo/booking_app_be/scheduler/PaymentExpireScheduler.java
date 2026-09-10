package com.vanmo.booking_app_be.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vanmo.booking_app_be.service.BookingService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentExpireScheduler {

    BookingService bookingService;

    @Scheduled(fixedDelay = 60000)
    public void markExpiredPendingPayments() {
        bookingService.markExpiredPendingPaymentsAsFailed();
    }
}
