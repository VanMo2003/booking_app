package com.vanmo.booking_app_be.dto.response;

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
 * An answer about one branch, written only from what that branch has published. {@link #answered} is false when the
 * question is about something the system does not hold (check-in time, refunds, a discount) - then the app offers to
 * put the question to the branch itself rather than letting the model guess.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiHotelAnswerResponse {

    Long hotelId;

    String question;

    String answer;

    /** False when the branch's published data does not cover the question. */
    boolean answered;

    /** Where to look next, only ever this branch's own page or one of its tours. */
    List<Link> links;

    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Link {

        /** "Xem tour Food tour phố cổ". */
        String label;

        /** A deep-link target the app understands: {@code tours/7} or {@code hotels/6}. */
        String target;
    }
}
