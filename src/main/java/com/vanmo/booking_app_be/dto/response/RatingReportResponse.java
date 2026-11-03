package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Guest reviews written in [from, to] (by review date) against the period before. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingReportResponse {
    LocalDate from;
    LocalDate to;
    long reviews;

    /** Average stars, 1 decimal; null without reviews. */
    Double averageRating;

    /** Same for the previous period (see the revenue summary's previous period); null without reviews then. */
    Double previousAverageRating;

    /** Reviews with 1, 2, 3, 4 and 5 stars, in that order. */
    List<Long> distribution;

    /** Reviews over the period's completed bookings, percentage - how many stays leave a review. */
    double reviewRate;

    /** The latest reviews with 3 stars or fewer (at most 5), newest first - the ones to follow up. */
    List<FeedbackRow> lowReviews;
}
