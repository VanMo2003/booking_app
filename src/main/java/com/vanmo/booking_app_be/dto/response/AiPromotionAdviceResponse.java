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
 * Discount campaigns worth running, read out of the chain's own figures. Nothing is created: the owner reads a
 * campaign, then writes the voucher on the voucher screen - which is the only thing that makes a real discount.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiPromotionAdviceResponse {

    Long hotelChainId;

    /** The period the figures were read from. */
    LocalDate from;

    LocalDate to;

    /** How the chain is doing, so the owner can judge the advice: occupancy and ADR of that period. */
    Double occupancyRate;

    BigDecimal adr;

    /** On-the-books occupancy for the window ahead - the hole a campaign would be filling. */
    Double outlookOccupancy;

    /** Vouchers already running, so the owner can see a campaign is not a duplicate. */
    List<String> runningVouchers;

    List<Campaign> campaigns;

    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Campaign {

        /** Short name for the campaign, e.g. "Giảm 10% đêm giữa tuần". */
        String title;

        /** CHAIN (every branch) or BRANCH (one branch only). */
        String scope;

        /** Set only for a BRANCH campaign, and only ever a branch of this chain. */
        Long hotelId;

        String hotelName;

        /** Percentage off, clamped in code - never the model's own number unchecked. */
        int discountPercent;

        /** Suggested minimum order, worked out from the period's ADR in code. */
        BigDecimal minOrderAmount;

        /** Dates computed from the horizon the model picked, not written by it. */
        LocalDate startsAt;

        LocalDate endsAt;

        /** Why this campaign, in one line, from the figures. */
        String why;

        /** Tours worth pushing with it; only real tours of the chain survive. */
        List<String> tours;
    }
}
