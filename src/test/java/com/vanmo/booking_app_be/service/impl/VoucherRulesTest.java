package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.HotelChain;
import com.vanmo.booking_app_be.entity.Voucher;
import com.vanmo.booking_app_be.entity.enums.DiscountType;

class VoucherRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    @Test
    void percentVouchersAreCappedAndAmountVouchersNeverGoBelowZero() {
        Voucher percent = voucher(DiscountType.PERCENT, 20);
        percent.setMaxDiscount(BigDecimal.valueOf(150_000));
        assertThat(VoucherRules.discount(percent, BigDecimal.valueOf(500_000))).isEqualByComparingTo("100000");
        assertThat(VoucherRules.discount(percent, BigDecimal.valueOf(1_000_000)))
                .isEqualByComparingTo("150000");

        Voucher amount = voucher(DiscountType.AMOUNT, 300_000);
        assertThat(VoucherRules.discount(amount, BigDecimal.valueOf(200_000))).isEqualByComparingTo("200000");
    }

    @Test
    void refusesOutsideItsDatesChainBranchLimitsAndMinimum() {
        Voucher voucher = voucher(DiscountType.PERCENT, 10);
        BigDecimal amount = BigDecimal.valueOf(500_000);
        assertThat(VoucherRules.refusal(voucher, 10L, 6L, amount, TODAY, 0)).isNull();

        assertThat(VoucherRules.refusal(voucher, 10L, 6L, amount, TODAY.plusMonths(2), 0))
                .contains("hết hạn");
        assertThat(VoucherRules.refusal(voucher, 11L, 7L, amount, TODAY, 0)).contains("không áp dụng");
        assertThat(VoucherRules.refusal(voucher, 10L, 6L, amount, TODAY, 1)).contains("đã dùng");

        voucher.setHotel(Hotel.builder().id(9L).build());
        assertThat(VoucherRules.refusal(voucher, 10L, 6L, amount, TODAY, 0)).contains("không áp dụng");
        voucher.setHotel(null);

        voucher.setUsageLimit(5);
        voucher.setUsedCount(5);
        assertThat(VoucherRules.refusal(voucher, 10L, 6L, amount, TODAY, 0)).contains("hết lượt");
        voucher.setUsageLimit(null);

        voucher.setMinOrderAmount(BigDecimal.valueOf(1_000_000));
        assertThat(VoucherRules.refusal(voucher, 10L, 6L, amount, TODAY, 0)).contains("từ 1000000");
    }

    private static Voucher voucher(DiscountType type, long value) {
        return Voucher.builder()
                .code("THU10")
                .title("Thử")
                .discountType(type)
                .value(BigDecimal.valueOf(value))
                .startsAt(TODAY.minusDays(1))
                .endsAt(TODAY.plusMonths(1))
                .hotelChain(HotelChain.builder().id(10L).build())
                .build();
    }
}
