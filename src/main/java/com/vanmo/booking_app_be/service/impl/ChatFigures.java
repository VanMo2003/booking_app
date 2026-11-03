package com.vanmo.booking_app_be.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.vanmo.booking_app_be.dto.response.AiAssistantReportResponse;
import com.vanmo.booking_app_be.dto.response.ChatMessageRow;
import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.dto.response.TourStatusCountRow;
import com.vanmo.booking_app_be.entity.enums.ChatSide;

/** Pure arithmetic of the chat assistant report, kept apart from the queries so it can be unit tested. */
final class ChatFigures {
    private ChatFigures() {}

    /**
     * @param messages the period's messages, thread by thread in the order they were written
     * @param placedByAi tour orders the assistant placed in the period, counted by status
     * @param completedTours completed tours of the period (by tour date), the assistant's and the others
     * @param waitingForStaff threads handed over and not answered yet
     */
    static AiAssistantReportResponse assistant(
            List<ChatMessageRow> messages,
            List<TourStatusCountRow> placedByAi,
            List<TourSaleRow> completedTours,
            long waitingForStaff) {
        Set<Long> conversations = new HashSet<>();
        Set<Long> answeredByAi = new HashSet<>();
        Set<Long> answeredByStaff = new HashSet<>();
        List<Long> aiWaits = new ArrayList<>();
        List<Long> staffWaits = new ArrayList<>();
        long guestMessages = 0;
        Long thread = null;
        Date waitingSince = null;
        for (ChatMessageRow message : messages) {
            if (!message.getConversationId().equals(thread)) {
                thread = message.getConversationId();
                waitingSince = null;
            }
            conversations.add(thread);
            if (message.getSenderSide() == ChatSide.CUSTOMER) {
                guestMessages++;
                // The wait runs from the first message the guest is still waiting on.
                if (waitingSince == null) {
                    waitingSince = message.getOnCreate();
                }
                continue;
            }
            boolean ai = Boolean.TRUE.equals(message.getAiGenerated());
            (ai ? answeredByAi : answeredByStaff).add(thread);
            if (waitingSince != null && message.getOnCreate() != null) {
                long seconds = Math.max(0, (message.getOnCreate().getTime() - waitingSince.getTime()) / 1000);
                (ai ? aiWaits : staffWaits).add(seconds);
            }
            waitingSince = null;
        }
        long aiReplies = countReplies(messages, true);
        long staffReplies = countReplies(messages, false);
        long aiOnly = answeredByAi.stream()
                .filter(id -> !answeredByStaff.contains(id))
                .count();

        long open = 0;
        long completed = 0;
        long canceled = 0;
        for (TourStatusCountRow row : placedByAi) {
            long count = row.getCount() == null ? 0 : row.getCount();
            switch (row.getStatus()) {
                case COMPLETED -> completed += count;
                case CANCELED -> canceled += count;
                default -> open += count;
            }
        }
        BigDecimal tourRevenue = RevenueFigures.sum(completedTours.stream().map(TourSaleRow::tourRevenue));
        BigDecimal aiTourRevenue = RevenueFigures.sum(completedTours.stream()
                .filter(t -> Boolean.TRUE.equals(t.getCreatedByAi()))
                .map(TourSaleRow::tourRevenue));

        return AiAssistantReportResponse.builder()
                .conversations(conversations.size())
                .aiOnlyConversations(aiOnly)
                .staffConversations(answeredByStaff.size())
                .guestMessages(guestMessages)
                .aiReplies(aiReplies)
                .staffReplies(staffReplies)
                .aiReplyShare(RevenueFigures.rate(aiReplies, aiReplies + staffReplies))
                .aiMedianReplySeconds(median(aiWaits))
                .staffMedianReplySeconds(median(staffWaits))
                .waitingForStaff(waitingForStaff)
                .aiTourOrders(open + completed + canceled)
                .aiTourOrdersOpen(open)
                .aiTourOrdersCompleted(completed)
                .aiTourOrdersCanceled(canceled)
                .aiTourRevenue(aiTourRevenue)
                .aiTourRevenueShare(share(aiTourRevenue, tourRevenue))
                .build();
    }

    private static long countReplies(List<ChatMessageRow> messages, boolean ai) {
        return messages.stream()
                .filter(m -> m.getSenderSide() == ChatSide.HOTEL)
                .filter(m -> Boolean.TRUE.equals(m.getAiGenerated()) == ai)
                .count();
    }

    /** Middle value (the mean of the two middle ones for an even count); null for none. */
    static Long median(List<Long> values) {
        List<Long> sorted =
                new ArrayList<>(values.stream().filter(Objects::nonNull).toList());
        if (sorted.isEmpty()) {
            return null;
        }
        Collections.sort(sorted);
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(mid) : (sorted.get(mid - 1) + sorted.get(mid)) / 2;
    }

    private static double share(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return 0d;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
