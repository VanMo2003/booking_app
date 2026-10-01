package com.vanmo.booking_app_be.controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vanmo.booking_app_be.config.VNPayConfig;
import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.dto.response.PaymentResponse;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.service.BookingService;
import com.vanmo.booking_app_be.service.PaymentService;
import com.vanmo.booking_app_be.utils.VNPayUtil;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {
    PaymentService paymentService;
    private final VNPayConfig vnPayConfig;
    private final BookingService bookingService;

    /**
     * Tạo URL thanh toán VNPay cho 1 booking
     *
     * FE gọi:
     * GET /payment/vn-pay?bookingId=123&amount=150000&bankCode=NCB
     *
     * - amount: đơn vị VND (ví dụ 150000). Service của bạn sẽ *100 theo chuẩn VNPay.
     * - bankCode: optional
     */
    @GetMapping("/vn-pay")
    public ApiResponse<PaymentResponse> pay(
            @RequestParam Long bookingId,
            @RequestParam Long amount,
            @RequestParam(required = false) String bankCode,
            HttpServletRequest request) {
        if (bookingId == null || bookingId <= 0) {
            return ApiResponse.fail("bookingId is required", 400);
        }
        if (amount == null || amount <= 0) {
            return ApiResponse.fail("amount must be > 0", 400);
        }

        return ApiResponse.ok(paymentService.createVnPayPayment(request, bookingId, amount, bankCode));
    }

    @GetMapping("/vn-pay-callback")
    public ApiResponse<?> vnpayReturn(HttpServletRequest request) {

        String secureHash = request.getParameter("vnp_SecureHash");

        Map<String, String> vnpParams = VNPayUtil.getVnpParams(request);
        boolean valid = VNPayUtil.verifySignature(vnpParams, vnPayConfig.getSecretKey(), secureHash);

        if (!valid) {
            return ApiResponse.fail("Invalid signature", 404);
        }

        String txnRef = request.getParameter("vnp_TxnRef"); // bookingId_random
        Long bookingId = Long.parseLong(txnRef.split("_")[0]);

        BookingResponse bookingResponse = bookingService.getById(bookingId);

        if (bookingResponse.getPaymentExpireAt() != null
                && bookingResponse.getPaymentExpireAt().isBefore(java.time.LocalDateTime.now())) {
            bookingService.updatePaymentStatus(bookingId, PaymentStatus.FAILED);
            return ApiResponse.ok("PAYMENT_EXPIRED");
        }

        String responseCode = request.getParameter("vnp_ResponseCode"); // "00" ok
        String transactionStatus = request.getParameter("vnp_TransactionStatus"); // "00" success

        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        if (success) {
            bookingService.markPaidByVnPay(bookingId);
        } else {
            bookingService.updatePaymentStatus(bookingId, PaymentStatus.UNPAID);
        }

        return ApiResponse.ok(success ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");
    }
}
