package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Natural-language tour search: the filters the model read out of the sentence, and the tours the backend matched
 * against real data. Results are ordinary {@link TourResponse} objects, so the app shows them with the same cards as
 * the rest of the tour screens.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiTourSearchResponse {

    String query;

    /** One-line reading of the request, e.g. "Tour biển Hạ Long · 2 người lớn 1 trẻ em · dưới 2.000.000 ₫". */
    String summary;

    Filters filters;

    List<TourResponse> results;

    /** What couldn't be applied (date in the past, nobody offers that theme…). */
    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filters {

        /** Places the guest named, plus the district or province they belong to when the model knows it. */
        List<String> destinations;

        Set<TravelTheme> themes;

        /** Per adult, the promotion applied. */
        BigDecimal minPrice;

        BigDecimal maxPrice;

        Integer adults;

        Integer children;

        /** The day the guest wants to go; seats are checked for real when it is set. */
        LocalDate tourDate;

        /** Nights of the stay the guest is up for; a day trip is 0. */
        Integer nights;

        /** True when the guest asked for a tour that comes with a room. */
        Boolean withRooms;

        /** Anything else worth matching against the tour's name and what it includes ("cano", "ăn chay"…). */
        List<String> keywords;
    }
}
