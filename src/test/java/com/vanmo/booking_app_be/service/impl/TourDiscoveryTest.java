package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

class TourDiscoveryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    @Test
    void featuredFallsBackToTheNewestToursWhenNoneIsFlagged() {
        List<Tour> tours = List.of(tour(1, false), tour(2, false), tour(3, true));

        assertThat(TourDiscovery.featured(tours)).extracting(Tour::getId).containsExactly(3L);
        assertThat(TourDiscovery.featured(List.of(tour(1, false), tour(2, false))))
                .extracting(Tour::getId)
                .containsExactly(2L, 1L);
    }

    @Test
    void dealsAreTheToursOnSaleTodayBiggestDiscountFirst() {
        Tour small = tour(1, false);
        small.setDiscountPercent(10);
        Tour big = tour(2, false);
        big.setDiscountPercent(30);
        Tour ended = tour(3, false);
        ended.setDiscountPercent(50);
        ended.setPromotionEndsAt(TODAY.minusDays(1));
        Tour lastDay = tour(4, false);
        lastDay.setDiscountPercent(20);
        lastDay.setPromotionEndsAt(TODAY);

        assertThat(TourDiscovery.deals(List.of(small, big, ended, lastDay), TODAY))
                .extracting(Tour::getId)
                .containsExactly(2L, 4L, 1L);
    }

    @Test
    void forYouRanksByHowManyInterestsATourMatches() {
        Tour beach = tour(1, false, TravelTheme.BEACH);
        Tour beachFood = tour(2, false, TravelTheme.BEACH, TravelTheme.FOOD);
        Tour mountain = tour(3, true, TravelTheme.MOUNTAIN);

        assertThat(TourDiscovery.forYou(
                        List.of(beach, beachFood, mountain), Set.of(TravelTheme.BEACH, TravelTheme.FOOD), TODAY))
                .extracting(Tour::getId)
                .containsExactly(2L, 1L);
        assertThat(TourDiscovery.forYou(List.of(beach), Set.of(), TODAY)).isEmpty();
    }

    @Test
    void salePriceTakesThePercentOffBothPrices() {
        Tour tour = tour(1, false);
        tour.setChildPrice(BigDecimal.valueOf(300_000));
        tour.setDiscountPercent(15);

        TourPricing.Price price = TourPricing.of(tour, TODAY);

        assertThat(price.onSale()).isTrue();
        assertThat(price.adult()).isEqualByComparingTo("425000");
        assertThat(price.child()).isEqualByComparingTo("255000");
        tour.setDiscountPercent(null);
        assertThat(TourPricing.of(tour, TODAY).child()).isEqualByComparingTo("300000");
    }

    private static Tour tour(long id, boolean featured, TravelTheme... themes) {
        Tour tour = Tour.builder()
                .id(id)
                .name("Tour " + id)
                .price(BigDecimal.valueOf(500_000))
                .featured(featured)
                .build();
        tour.getThemes().addAll(Set.of(themes));
        return tour;
    }
}
