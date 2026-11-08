package com.vanmo.booking_app_be.service.impl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

/** Which tours the home screen shows, kept apart from the queries so it can be unit tested. */
final class TourDiscovery {
    static final int SHELF = 10;

    private TourDiscovery() {}

    /** Tours the branches flagged as featured, newest first; the newest tours when none is flagged. */
    static List<Tour> featured(List<Tour> tours) {
        List<Tour> flagged = tours.stream().filter(Tour::isFeatured).toList();
        return (flagged.isEmpty() ? tours : flagged)
                .stream()
                        .sorted(Comparator.comparing(Tour::getId).reversed())
                        .limit(SHELF)
                        .toList();
    }

    /** Tours on sale today, biggest discount first. */
    static List<Tour> deals(List<Tour> tours, LocalDate today) {
        return tours.stream()
                .filter(tour -> TourPricing.onSale(tour, today))
                .sorted(Comparator.comparing(Tour::getDiscountPercent)
                        .reversed()
                        .thenComparing(Tour::getId, Comparator.reverseOrder()))
                .limit(SHELF)
                .toList();
    }

    /** Tours sharing the most themes with the guest's interests, then featured, then on sale. */
    static List<Tour> forYou(List<Tour> tours, Set<TravelTheme> interests, LocalDate today) {
        if (interests == null || interests.isEmpty()) {
            return List.of();
        }
        return tours.stream()
                .filter(tour -> matches(tour, interests) > 0)
                .sorted(Comparator.comparingLong((Tour tour) -> matches(tour, interests))
                        .reversed()
                        .thenComparing(Tour::isFeatured, Comparator.reverseOrder())
                        .thenComparing(tour -> TourPricing.onSale(tour, today), Comparator.reverseOrder())
                        .thenComparing(Tour::getId, Comparator.reverseOrder()))
                .limit(SHELF)
                .toList();
    }

    static long matches(Tour tour, Set<TravelTheme> interests) {
        return tour.getThemes() == null
                ? 0
                : tour.getThemes().stream().filter(interests::contains).count();
    }
}
