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
 * How ready a branch's page is to sell: the checks the backend ran over its own rows, a score computed from them,
 * and the assistant's wording of what to fix first. Every "fact" here is counted in code; the model only explains
 * and orders them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiContentAuditResponse {

    Long hotelId;

    /** 0-100, from the checks below - not the model's opinion. */
    int score;

    /** Two or three sentences on where the page stands. */
    String summary;

    /** Every check, passed or not, so the manager sees the whole picture. */
    List<Check> checks;

    /** What to fix first, in the assistant's words. */
    List<Item> items;

    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Check {

        /** Stable key, e.g. {@code rooms_with_photo}. */
        String key;

        /** What it checks, in Vietnamese. */
        String label;

        boolean passed;

        /** How many rows passed and how many there are, when the check counts rows. */
        Integer done;

        Integer total;

        /** Why it matters, one short line. */
        String hint;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {

        String title;

        /** HIGH, MEDIUM or LOW. */
        String priority;

        /** What it costs the branch now. */
        String why;

        /** What to do, concretely. */
        String how;

        /** The check this advice is about; only keys from {@link AiContentAuditResponse#checks} survive. */
        String checkKey;
    }
}
