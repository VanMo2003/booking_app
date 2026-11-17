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
 * A branch's own published text in another language. Each item carries the original beside the translation, so a
 * guest (or the branch) can always see what was translated, and nothing new can appear in the list: the items are
 * the branch's rows, and the model only fills in {@link Item#translated}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiTranslationResponse {

    Long hotelId;

    /** Language code asked for, e.g. {@code en}. */
    String lang;

    /** What that code means, for the app's heading: "English". */
    String language;

    List<Item> items;

    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {

        /** Stable key the model answers with, e.g. {@code dish_12}. */
        String key;

        /** Which part of the page this came from: DESCRIPTION, AMENITY, SERVICE, DISH, TOUR. */
        String section;

        /** The branch's own text. */
        String original;

        /** The translation; null when the model skipped this item, and the app then shows the original. */
        String translated;
    }
}
