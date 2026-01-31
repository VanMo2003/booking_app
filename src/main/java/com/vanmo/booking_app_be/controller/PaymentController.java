package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.response.PaymentResponse;
import com.vanmo.booking_app_be.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {
    PaymentService paymentService;
    @GetMapping("/vn-pay")
    public ApiResponse<PaymentResponse> pay(HttpServletRequest request) {
        return ApiResponse.ok(paymentService.createVnPayPayment(request));
    }
    @GetMapping("/vn-pay-callback")
    public ApiResponse<PaymentResponse> payCallbackHandler(HttpServletRequest request) {
        String status = request.getParameter("vnp_ResponseCode");
        if (status.equals("00")) {
            return ApiResponse.ok(new PaymentResponse("00", "Success", ""));
        } else {
            return ApiResponse.fail("Failed", 0);
        }
    }
}
