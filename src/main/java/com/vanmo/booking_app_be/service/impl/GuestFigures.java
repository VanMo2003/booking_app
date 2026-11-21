package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.LongStream;

import com.vanmo.booking_app_be.dto.response.FeedbackRow;
import com.vanmo.booking_app_be.dto.response.GuestOriginDTO;
import com.vanmo.booking_app_be.dto.response.GuestOriginRow;
import com.vanmo.booking_app_be.dto.response.RatingReportResponse;

/** Pure arithmetic of the guest reports (where guests come from, what they rate), kept apart for unit tests. */
final class GuestFigures {
    /** A review with this many stars or fewer is one to follow up. */
    static final int LOW_RATING = 3;

    private static final int LOW_REVIEWS_SHOWN = 5;

    private GuestFigures() {}

    /**
     * Groups completed bookings and tours by the guest's hometown, as typed: spaces and letter case are ignored, the
     * first spelling seen is shown. Guests without one are grouped under a null hometown. Biggest revenue first.
     */
    static List<GuestOriginDTO> origins(List<GuestOriginRow> bookings, List<GuestOriginRow> tours) {
        Map<String, GuestOriginDTO> byPlace = new LinkedHashMap<>();
        Map<String, Set<Long>> guests = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int part = 0; part < 2; part++) {
            boolean tour = part == 1;
            for (GuestOriginRow row : tour ? tours : bookings) {
                String place = clean(row.getHometown());
                String key = place == null ? "" : place.toLowerCase(Locale.ROOT);
                GuestOriginDTO origin = byPlace.computeIfAbsent(key, k -> GuestOriginDTO.builder()
                        .hometown(place)
                        .revenue(BigDecimal.ZERO)
                        .build());
                if (tour) {
                    origin.setTours(origin.getTours() + 1);
                } else {
                    origin.setBookings(origin.getBookings() + 1);
                }
                BigDecimal amount = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
                origin.setRevenue(origin.getRevenue().add(amount));
                total = total.add(amount);
                guests.computeIfAbsent(key, k -> new HashSet<>()).add(row.getCustomerId());
            }
        }
        for (Map.Entry<String, GuestOriginDTO> entry : byPlace.entrySet()) {
            GuestOriginDTO origin = entry.getValue();
            origin.setGuests(guests.get(entry.getKey()).size());
            origin.setShare(share(origin.getRevenue(), total));
        }
        return byPlace.values().stream()
                .sorted(Comparator.comparing(GuestOriginDTO::getRevenue)
                        .reversed()
                        .thenComparing(GuestOriginDTO::getGuests, Comparator.reverseOrder()))
                .toList();
    }

    /**
     * @param reviews the period's reviews, newest first
     * @param previous the previous period's reviews
     * @param completedBookings completed bookings of the period, for the share that left a review
     */
    static RatingReportResponse ratings(
            LocalDate from,
            LocalDate to,
            List<FeedbackRow> reviews,
            List<FeedbackRow> previous,
            long completedBookings) {
        long[] stars = new long[5];
        for (FeedbackRow review : reviews) {
            if (review.getRating() != null && review.getRating() >= 1 && review.getRating() <= 5) {
                stars[review.getRating() - 1]++;
            }
        }
        List<FeedbackRow> low = new ArrayList<>();
        for (FeedbackRow review : reviews) {
            if (low.size() < LOW_REVIEWS_SHOWN && review.getRating() != null && review.getRating() <= LOW_RATING) {
                low.add(review);
            }
        }
        return RatingReportResponse.builder()
                .from(from)
                .to(to)
                .reviews(reviews.size())
                .averageRating(average(reviews))
                .previousAverageRating(average(previous))
                .distribution(LongStream.of(stars).boxed().toList())
                .reviewRate(RevenueFigures.rate(reviews.size(), completedBookings))
                .lowReviews(low)
                .build();
    }

    /** Average stars with one decimal; null without a rated review. */
    static Double average(List<FeedbackRow> reviews) {
        List<Integer> ratings = reviews.stream()
                .map(FeedbackRow::getRating)
                .filter(Objects::nonNull)
                .toList();
        if (ratings.isEmpty()) {
            return null;
        }
        double mean = ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(mean * 10) / 10d;
    }

    private static String clean(String hometown) {
        if (hometown == null) {
            return null;
        }
        String text = hometown.strip().replaceAll("\s+", " ");
        return text.isEmpty() ? null : text;
    }

    private static double share(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return 0d;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
