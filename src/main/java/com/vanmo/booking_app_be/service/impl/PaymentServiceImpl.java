package com.vanmo.booking_app_be.service.impl;

import com.vanmo.booking_app_be.config.VNPayConfig;
import com.vanmo.booking_app_be.dto.response.PaymentResponse;
import com.vanmo.booking_app_be.service.PaymentService;
import com.vanmo.booking_app_be.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {
    VNPayConfig vnPayConfig;

    @Override
    public PaymentResponse createVnPayPayment(HttpServletRequest request, Long bookingId) {
        long amount = Long.parseLong(request.getParameter("amount")) * 100L;
        String bankCode = request.getParameter("bankCode");

        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig();

        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(vietnamZone);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        vnpParamsMap.put("vnp_CreateDate", now.format(formatter));
        vnpParamsMap.put("vnp_ExpireDate", now.plusMinutes(15).format(formatter));

        String txnRef = bookingId + "_" + VNPayUtil.getRandomNumber(8);
        vnpParamsMap.put("vnp_TxnRef", txnRef);

        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));

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