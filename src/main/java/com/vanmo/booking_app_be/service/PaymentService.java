package com.vanmo.booking_app_be.service;

import jakarta.servlet.http.HttpServletRequest;

import com.vanmo.booking_app_be.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createVnPayPayment(HttpServletRequest request, Long bookingId, Long amount, String bankCode);
}
