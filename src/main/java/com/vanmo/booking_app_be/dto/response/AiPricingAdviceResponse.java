package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
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
 * What the assistant suggests doing with a branch's room prices, per room type, from that branch's own numbers. It
 * is advice only - nothing changes a room's price - and every item carries the figures it was based on so the
 * manager can see why.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiPricingAdviceResponse {

    Long hotelId;

    /** The period the figures come from. */
    LocalDate from;

    LocalDate to;

    /** Days ahead the "already booked" figures cover. */
    int outlookDays;

    /** Two or three sentences on where the branch stands. */
    String summary;

    List<Item> items;

    /** What could go wrong with following the advice. */
    List<String> risks;

    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {

        Long roomTypeId;

        String roomTypeName;

        /** RAISE, HOLD or LOWER. */
        String action;

        /** How much to move the price, in percent (1-30); null for HOLD. */
        Integer percent;

        String reason;

        /** The figures the advice was based on, straight from the report. */
        BigDecimal listPrice;

        BigDecimal adr;

        double occupancyRate;

        long rooms;

        /** listPrice moved by percent - what the manager would set it to. */
        BigDecimal suggestedPrice;
    }
}
