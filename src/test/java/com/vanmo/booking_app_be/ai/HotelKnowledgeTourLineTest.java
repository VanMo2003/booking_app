package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.entity.Tour;

/**
 * The tour lines of the fact sheet are the only prices the chat assistant quotes, so they must be the prices an
 * order is actually charged at: today's price through {@code TourPricing}, not the list price, and the child price
 * when the tour has one. Quoting the list price for a tour on sale overcharges the guest in writing.
 */
class HotelKnowledgeTourLineTest {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 20);

    private static Tour tour() {
        Tour tour = new Tour();
        tour.setId(12L);
        tour.setName("Food tour phố cổ");
        tour.setAvailable(true);
        tour.setPrice(new BigDecimal("450000"));
        return tour;
    }

    @Test
    void aPlainTourIsQuotedAtItsListPricePerAdult() {
        String line = HotelKnowledge.tourLine(tour(), List.of(), DAY);

        assertThat(line).startsWith("- [mã 12] Food tour phố cổ: 450.000 ₫/người lớn");
        assertThat(line).doesNotContain("đang giảm").doesNotContain("trẻ em");
    }

    @Test
    void aTourOnSaleIsQuotedAtTheSalePriceWithTheOriginalShown() {
        Tour tour = tour();
        tour.setDiscountPercent(20);
        tour.setPromotionEndsAt(DAY.plusDays(3));

        assertThat(HotelKnowledge.tourLine(tour, List.of(), DAY))
                .contains("360.000 ₫/người lớn (đang giảm 20%, giá gốc 450.000 ₫)");
    }

    @Test
    void aPromotionThatEndedYesterdayIsBackToTheListPrice() {
        Tour tour = tour();
        tour.setDiscountPercent(20);
        tour.setPromotionEndsAt(DAY.minusDays(1));

        assertThat(HotelKnowledge.tourLine(tour, List.of(), DAY))
                .contains("450.000 ₫/người lớn")
                .doesNotContain("đang giảm");
    }

    @Test
    void aChildPriceIsListedAndDiscountedTogetherWithTheAdultOne() {
        Tour tour = tour();
        tour.setChildPrice(new BigDecimal("300000"));

        assertThat(HotelKnowledge.tourLine(tour, List.of(), DAY)).contains("; 300.000 ₫/trẻ em");

        tour.setDiscountPercent(20);
        assertThat(HotelKnowledge.tourLine(tour, List.of(), DAY))
                .contains("360.000 ₫/người lớn")
                .contains("; 240.000 ₫/trẻ em");
    }

    @Test
    void aPausedTourStillSaysSoBeforeItsPrice() {
        Tour tour = tour();
        tour.setAvailable(false);

        assertThat(HotelKnowledge.tourLine(tour, List.of(), DAY))
                .contains("(TẠM NGỪNG, không nhận khách): 450.000 ₫/người lớn");
    }
}
