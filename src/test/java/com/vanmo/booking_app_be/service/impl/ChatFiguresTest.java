package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.dto.response.AiAssistantReportResponse;
import com.vanmo.booking_app_be.dto.response.ChatMessageRow;
import com.vanmo.booking_app_be.dto.response.TourSaleRow;
import com.vanmo.booking_app_be.dto.response.TourStatusCountRow;
import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;

class ChatFiguresTest {

    private static final long T0 = 1_790_000_000_000L;
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Test
    void separatesAssistantAndTeamRepliesAndTimesEachFromTheFirstUnansweredMessage() {
        List<ChatMessageRow> messages = List.of(
                // Thread 1: the assistant answers both questions on its own.
                guest(1, 0),
                guest(1, 30), // still waiting: the wait keeps running from 0
                ai(1, 40),
                guest(1, 100),
                ai(1, 120),
                // Thread 2: the assistant answers, then the team takes over.
                guest(2, 0),
                ai(2, 10),
                guest(2, 60),
                staff(2, 660),
                staff(2, 700), // a follow-up, not an answer to anything
                // Thread 3: nobody answered yet.
                guest(3, 500));

        AiAssistantReportResponse report = ChatFigures.assistant(messages, List.of(), List.of(), 2);

        assertThat(report.getConversations()).isEqualTo(3);
        assertThat(report.getAiOnlyConversations()).isEqualTo(1);
        assertThat(report.getStaffConversations()).isEqualTo(1);
        assertThat(report.getGuestMessages()).isEqualTo(6);
        assertThat(report.getAiReplies()).isEqualTo(3);
        assertThat(report.getStaffReplies()).isEqualTo(2);
        assertThat(report.getAiReplyShare()).isEqualTo(60.0);
        // Assistant waits 40, 20, 10 s; the team's single answer took 600 s.
        assertThat(report.getAiMedianReplySeconds()).isEqualTo(20L);
        assertThat(report.getStaffMedianReplySeconds()).isEqualTo(600L);
        assertThat(report.getWaitingForStaff()).isEqualTo(2);
    }

    @Test
    void aWaitDoesNotCarryOverIntoTheNextThread() {
        List<ChatMessageRow> messages = List.of(guest(1, 0), staff(2, 50));

        AiAssistantReportResponse report = ChatFigures.assistant(messages, List.of(), List.of(), 0);

        assertThat(report.getStaffReplies()).isEqualTo(1);
        assertThat(report.getStaffMedianReplySeconds()).isNull();
        assertThat(report.getAiMedianReplySeconds()).isNull();
        assertThat(report.getAiReplyShare()).isZero();
    }

    @Test
    void countsTheAssistantsTourOrdersAndItsShareOfTourRevenue() {
        List<TourStatusCountRow> placed = List.of(
                new TourStatusCountRow(TourBookingStatus.PENDING, null, 1L),
                new TourStatusCountRow(TourBookingStatus.CONFIRMED, null, 2L),
                new TourStatusCountRow(TourBookingStatus.COMPLETED, null, 3L),
                new TourStatusCountRow(TourBookingStatus.CANCELED, ChatSide.CUSTOMER, 1L),
                new TourStatusCountRow(TourBookingStatus.CANCELED, ChatSide.HOTEL, 1L));
        List<TourSaleRow> completed = List.of(tour(300_000, true), tour(100_000, true), tour(600_000, false));

        AiAssistantReportResponse report = ChatFigures.assistant(List.of(), placed, completed, 0);

        assertThat(report.getAiTourOrders()).isEqualTo(8);
        assertThat(report.getAiTourOrdersOpen()).isEqualTo(3);
        assertThat(report.getAiTourOrdersCompleted()).isEqualTo(3);
        assertThat(report.getAiTourOrdersCanceled()).isEqualTo(2);
        assertThat(report.getAiTourRevenue()).isEqualByComparingTo("400000");
        assertThat(report.getAiTourRevenueShare()).isEqualTo(40.0);
        assertThat(report.getConversations()).isZero();
    }

    @Test
    void medianOfAnEvenCountIsTheMeanOfTheMiddleTwo() {
        assertThat(ChatFigures.median(List.of(40L, 10L, 20L, 90L))).isEqualTo(30L);
        assertThat(ChatFigures.median(List.of())).isNull();
    }

    private static ChatMessageRow guest(long thread, int second) {
        return new ChatMessageRow(thread, ChatSide.CUSTOMER, null, at(second));
    }

    private static ChatMessageRow ai(long thread, int second) {
        return new ChatMessageRow(thread, ChatSide.HOTEL, true, at(second));
    }

    private static ChatMessageRow staff(long thread, int second) {
        return new ChatMessageRow(thread, ChatSide.HOTEL, null, at(second));
    }

    private static Date at(int second) {
        return new Date(T0 + second * 1000L);
    }

    private static TourSaleRow tour(long amount, boolean byAi) {
        return new TourSaleRow(
                1L, "Hạ Long", 2, BigDecimal.valueOf(amount), BigDecimal.ZERO, BigDecimal.valueOf(amount), byAi, OCT_1);
    }
}
