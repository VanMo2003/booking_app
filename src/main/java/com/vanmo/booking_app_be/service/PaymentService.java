package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.dto.response.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {
    PaymentResponse createVnPayPayment(HttpServletRequest request, Long bookingId);
}
