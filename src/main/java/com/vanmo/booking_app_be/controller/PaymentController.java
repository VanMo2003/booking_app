package com.vanmo.booking_app_be.controller;

import com.vanmo.booking_app_be.config.VNPayConfig;
import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.response.PaymentResponse;
import com.vanmo.booking_app_be.entity.enums.PaymentStatus;
import com.vanmo.booking_app_be.service.BookingService;
import com.vanmo.booking_app_be.service.PaymentService;
import com.vanmo.booking_app_be.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
            HttpServletRequest request
    ) {
        if (bookingId == null || bookingId <= 0) {
            return ApiResponse.fail("bookingId is required", 400);
        }
        if (amount == null || amount <= 0) {
            return ApiResponse.fail("amount must be > 0", 400);
        }

        // Gán vào request parameter để PaymentServiceImpl lấy y như code hiện tại
        // (vì bạn đang dùng request.getParameter("amount") / ("bankCode"))
        request.setAttribute("amount", String.valueOf(amount));
        if (bankCode != null) request.setAttribute("bankCode", bankCode);

        // ✅ (khuyến nghị) bạn nên cập nhật PaymentServiceImpl để nhận amount/bankCode trực tiếp
        // Nhưng với code hiện tại, bạn có 2 lựa chọn:
        // 1) đổi PaymentServiceImpl để nhận amount/bankCode param
        // 2) giữ nguyên và FE truyền amount/bankCode trong query như dưới

        // Nếu bạn giữ nguyên PaymentServiceImpl: chỉ cần FE truyền amount/bankCode vào query string,
        // thì request.getParameter vẫn đọc được => không cần setAttribute.
        // Mình vẫn để setAttribute cho rõ ý, nhưng bạn có thể xóa 2 dòng setAttribute ở trên.

        // (optional) check booking tồn tại (tuỳ BookingService của bạn có method check không)
        // bookingService.getById(bookingId);

        return ApiResponse.ok(paymentService.createVnPayPayment(request, bookingId));
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

        String responseCode = request.getParameter("vnp_ResponseCode");          // "00" ok
        String transactionStatus = request.getParameter("vnp_TransactionStatus"); // "00" success

        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        if (success) {
            bookingService.updatePaymentStatus(bookingId, PaymentStatus.PAID);
        } else {
            bookingService.updatePaymentStatus(bookingId, PaymentStatus.UNPAID);
        }

        return ApiResponse.ok(success ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");
    }
}
