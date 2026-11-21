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
 * What a branch has to deal with today. The signals are counted in code from the branch's own rows; the model only
 * puts them in order and says, in a sentence each, why one of them is worth doing before the others.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiDailyBriefResponse {

    Long hotelId;

    /** The day the brief was built for (Vietnam time). */
    LocalDate date;

    /** One sentence on how the day looks; absent when AI is off. */
    String headline;

    /** Every signal, quiet ones included, so the manager sees the whole desk. */
    List<Signal> signals;

    /** What to do first, in the assistant's words - only ever about a signal that is not quiet. */
    List<Task> tasks;

    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Signal {

        /** Stable key, e.g. {@code pending_bookings}. */
        String key;

        /** What it counts, in Vietnamese. */
        String label;

        /** The figure itself, already worded ("3 đơn", "1,2 tr đ"). */
        String value;

        /** One line of context: how old, how much, which guest. */
        String detail;

        /** URGENT (needs a person today), WATCH (worth a look), QUIET (nothing to do). */
        String level;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Task {

        String title;

        /** HIGH, MEDIUM or LOW. */
        String priority;

        /** What happens if it waits. */
        String why;

        /** What to do, concretely. */
        String how;

        /** The signal this task is about; only keys from a non-quiet {@link Signal} survive. */
        String signalKey;
    }
}
