package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Natural-language search: the filters the model understood, and branches the backend matched against real data. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiSearchResponse {
    String query;

    /** One-line reading of the request, e.g. "Phòng cho 4 khách · 04/10–06/10 · dưới 1.000.000 ₫". */
    String summary;

    Filters filters;
    List<Result> results;

    /** What couldn't be applied (unknown amenity, date in the past…). */
    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Filters {
        LocalDate checkin;
        LocalDate checkout;
        Integer guests;
        BigDecimal minPrice;
        BigDecimal maxPrice;
        List<String> areas;
        List<String> amenities;
        String category;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Result {
        Long hotelId;
        String name;
        String address;
        String category;
        double rating;
        String pathImage;
        int matchingRooms;
        BigDecimal fromPrice;
        List<String> matchedAmenities;
    }
}
