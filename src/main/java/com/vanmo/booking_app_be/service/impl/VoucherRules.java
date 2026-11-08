package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.vanmo.booking_app_be.entity.Voucher;
import com.vanmo.booking_app_be.entity.enums.DiscountType;

/** Whether a voucher applies to an order and how much it takes off - pure, unit-tested. */
final class VoucherRules {
    private VoucherRules() {}

    /** Why the voucher cannot be used for this order, or null when it can. */
    static String refusal(
            Voucher voucher, Long chainId, Long hotelId, BigDecimal amount, LocalDate today, long usedByCustomer) {
        if (!voucher.isActive()) {
            return "Mã " + voucher.getCode() + " đã ngừng áp dụng.";
        }
        if (today.isBefore(voucher.getStartsAt())) {
            return "Mã " + voucher.getCode() + " chưa đến ngày áp dụng.";
        }
        if (today.isAfter(voucher.getEndsAt())) {
            return "Mã " + voucher.getCode() + " đã hết hạn.";
        }
        if (!voucher.getHotelChain().getId().equals(chainId)
                || (voucher.getHotel() != null && !voucher.getHotel().getId().equals(hotelId))) {
            return "Mã " + voucher.getCode() + " không áp dụng cho tour này.";
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return "Mã " + voucher.getCode() + " đã hết lượt dùng.";
        }
        if (usedByCustomer >= voucher.getPerCustomerLimit()) {
            return "Bạn đã dùng mã " + voucher.getCode() + " rồi.";
        }
        if (voucher.getMinOrderAmount() != null && amount.compareTo(voucher.getMinOrderAmount()) < 0) {
            return "Mã " + voucher.getCode() + " áp dụng cho đơn tour từ "
                    + voucher.getMinOrderAmount()
                            .setScale(0, RoundingMode.HALF_UP)
                            .toPlainString() + " ₫.";
        }
        return null;
    }

    /** What the voucher takes off the amount: never more than the amount itself. */
    static BigDecimal discount(Voucher voucher, BigDecimal amount) {
        BigDecimal off;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            off = amount.multiply(voucher.getValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount().signum() > 0) {
                off = off.min(voucher.getMaxDiscount());
            }
        } else {
            off = voucher.getValue();
        }
        return off.min(amount).max(BigDecimal.ZERO);
    }
}
