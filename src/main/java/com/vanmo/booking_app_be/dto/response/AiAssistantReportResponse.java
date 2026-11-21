package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * What the chat assistant did over [from, to]: the conversations it answered against the ones the team stepped into,
 * how fast each side replied, and the tour orders it took.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiAssistantReportResponse {
    /** Conversations with at least one message in the period. */
    long conversations;

    /** Of those, conversations the assistant answered with no one on the team writing. */
    long aiOnlyConversations;

    /** Conversations someone on the team wrote in. */
    long staffConversations;

    long guestMessages;

    long aiReplies;

    long staffReplies;

    /** aiReplies over all replies from the hotel side, percentage. */
    double aiReplyShare;

    /**
     * Median time from a guest's first unanswered message to the next reply by the assistant / by the team; null when
     * that side never replied.
     */
    Long aiMedianReplySeconds;

    Long staffMedianReplySeconds;

    /** Conversations the assistant handed over that no one on the team has answered yet (now, not in the period). */
    long waitingForStaff;

    /** Tour orders the assistant placed in the period, by where they are now. */
    long aiTourOrders;

    long aiTourOrdersOpen;

    long aiTourOrdersCompleted;

    long aiTourOrdersCanceled;

    /** Completed tours the assistant sold, by tour date (same basis as the revenue reports). */
    BigDecimal aiTourRevenue;

    /** aiTourRevenue over all completed tour revenue of the period, percentage. */
    double aiTourRevenueShare;
}
