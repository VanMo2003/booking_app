package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
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
 * Two to four branches side by side. Every figure in {@link Item} is counted from the branches' own rows; the
 * assistant only writes who each one suits and what the real differences are.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiHotelCompareResponse {

    /** One row per branch, in the order they were asked for. */
    List<Item> items;

    /** What actually differs between them, one short line each. */
    List<String> differences;

    /** Two or three sentences: which to pick for what. */
    String verdict;

    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {

        Long hotelId;

        String name;

        String category;

        String address;

        /** Cheapest and dearest room's list price, null when the branch has no priced room. */
        BigDecimal priceFrom;

        BigDecimal priceTo;

        /** How many guests the largest room sleeps. */
        int maxCapacity;

        int rooms;

        int amenities;

        int services;

        int tours;

        /** From the branch's own reviews, not the stored rating field. */
        int reviews;

        double averageRating;

        /** Who this branch suits, in the assistant's words. */
        String fitFor;
    }
}
