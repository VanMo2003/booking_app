package com.vanmo.booking_app_be.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.config.VNPayConfig;
import com.vanmo.booking_app_be.dto.response.PaymentResponse;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.PaymentMethod;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.service.PaymentService;
import com.vanmo.booking_app_be.utils.VNPayUtil;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {
    VNPayConfig vnPayConfig;
    BookingRepository bookingRepository;

    @Override
    public PaymentResponse createVnPayPayment(
            HttpServletRequest request, Long bookingId, Long amount, String bankCode) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + bookingId));

        if (booking.getPaymentMethod() != PaymentMethod.VN_PAY) {
            throw new IllegalStateException("Booking does not use VN_PAY");
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Booking already paid");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELED) {
            throw new IllegalStateException("Booking is cancelled");
        }

        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking is completed");
        }

        if (booking.getPaymentStatus() == PaymentStatus.FAILED
                || (booking.getPaymentExpireAt() != null
                        && booking.getPaymentExpireAt().isBefore(LocalDateTime.now()))) {
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setPaymentExpireAt(LocalDateTime.now().plusMinutes(15));
            booking.setPaidAt(null);
            bookingRepository.saveAndFlush(booking);
        }

        if (booking.getPaymentStatus() == PaymentStatus.PENDING) {
            booking.setPaymentExpireAt(LocalDateTime.now().plusMinutes(15));
            bookingRepository.saveAndFlush(booking);
        }

        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();

        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(vietnamZone);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        vnpParamsMap.put("vnp_CreateDate", now.format(formatter));
        vnpParamsMap.put("vnp_ExpireDate", now.plusMinutes(15).format(formatter));

        String txnRef = bookingId + "_" + VNPayUtil.getRandomNumber(8);
        vnpParamsMap.put("vnp_TxnRef", txnRef);

        vnpParamsMap.put("vnp_Amount", String.valueOf(amount * 100L));
        vnpParamsMap.put("vnp_OrderInfo", "Thanh toan booking " + bookingId);

        if (bankCode != null && !bankCode.isEmpty()) vnpParamsMap.put("vnp_BankCode", bankCode);

        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
        String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

        return PaymentResponse.builder()
                .code("oke")
                .message("success")
                .paymentUrl(paymentUrl)
                .build();
    }
}
