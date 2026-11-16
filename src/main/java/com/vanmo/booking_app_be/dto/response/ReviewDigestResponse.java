package com.vanmo.booking_app_be.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * What guests keep saying about a branch. Read out of that branch's own reviews, so every line comes from something
 * a guest actually wrote; {@code reviewCount} says how many it is based on.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewDigestResponse {

    Long hotelId;

    /** False when the branch has too few reviews yet, or the server has no AI key: the app hides the card. */
    boolean available;

    String summary;

    List<String> pros;

    List<String> cons;

    int reviewCount;

    double averageRating;

    /** Rating 1-5 → how many reviews gave it, index 0 = one star. */
    List<Integer> ratingCounts;

    Instant generatedAt;
}
