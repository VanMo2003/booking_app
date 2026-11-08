package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;

import com.vanmo.booking_app_be.entity.Tour;

/** What a tour costs per adult and per child on a day, its promotion applied - shown to guests and charged on orders. */
public final class TourPricing {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private TourPricing() {}

    public record Price(BigDecimal adult, BigDecimal child, boolean onSale) {}

    public static LocalDate today() {
        return LocalDate.now(VIETNAM);
    }

    /** The promotion runs from now until its end date (inclusive), when a discount is set. */
    public static boolean onSale(Tour tour, LocalDate day) {
        Integer percent = tour.getDiscountPercent();
        return percent != null
                && percent > 0
                && (tour.getPromotionEndsAt() == null || !day.isAfter(tour.getPromotionEndsAt()));
    }

    public static Price of(Tour tour, LocalDate day) {
        BigDecimal adult = tour.getPrice();
        BigDecimal child = tour.getChildPrice() == null ? adult : tour.getChildPrice();
        if (!onSale(tour, day)) {
            return new Price(adult, child, false);
        }
        int percent = tour.getDiscountPercent();
        return new Price(discounted(adult, percent), discounted(child, percent), true);
    }

    static BigDecimal discounted(BigDecimal amount, int percent) {
        if (amount == null) {
            return null;
        }
        return amount.multiply(BigDecimal.valueOf(100L - percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }
}
