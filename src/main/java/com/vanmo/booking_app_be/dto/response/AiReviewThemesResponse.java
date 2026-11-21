package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;
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
 * What guests across a chain keep talking about, topic by topic, read out of the chain's own reviews for a period.
 * Every branch named in a theme is a real branch of the chain, and the per-branch counts come from the rows, not
 * from the model.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiReviewThemesResponse {

    Long hotelChainId;

    LocalDate from;

    LocalDate to;

    /** Reviews with a comment that the themes were read from. */
    int reviewCount;

    /** Two or three sentences on what guests keep saying across the chain. */
    String summary;

    List<Theme> themes;

    /** What the owner could do about it, one line each. */
    List<String> actions;

    /** Reviews and average stars per branch over the period, computed from the rows. */
    List<BranchReviews> byBranch;

    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Theme {

        /** "Sạch sẽ", "Thái độ nhân viên", "Tiếng ồn"… */
        String topic;

        /** GOOD, MIXED or BAD. */
        String sentiment;

        /** One sentence, in the guests' own terms. */
        String note;

        /** Branches of the chain where guests raised it; only real branch names survive. */
        List<String> branches;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BranchReviews {

        Long hotelId;

        String hotelName;

        int reviews;

        /** Average stars, one decimal; null when that branch had no review in the period. */
        Double averageRating;
    }
}
