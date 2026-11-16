package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The gate in front of both booking tools: the assistant may place a room booking or a tour order only when the
 * guest's own latest message reads as "yes". A model that skips the summary step books nothing.
 */
class ChatAssistantConsentTest {

    @Test
    void readsTheUsualVietnameseYesWithOrWithoutAccents() {
        assertThat(ChatAssistant.looksLikeConsent("Đồng ý")).isTrue();
        assertThat(ChatAssistant.looksLikeConsent("dong y nhe")).isTrue();
        assertThat(ChatAssistant.looksLikeConsent("ok bạn")).isTrue();
        assertThat(ChatAssistant.looksLikeConsent("Xác nhận giúp mình")).isTrue();
        assertThat(ChatAssistant.looksLikeConsent("chốt luôn")).isTrue();
        assertThat(ChatAssistant.looksLikeConsent("vâng")).isTrue();
        assertThat(ChatAssistant.looksLikeConsent("đặt giúp mình nhé")).isTrue();
        assertThat(ChatAssistant.looksLikeConsent("yes please")).isTrue();
    }

    @Test
    void aQuestionOrAChangeIsNotAYes() {
        assertThat(ChatAssistant.looksLikeConsent("Phòng này bao nhiêu tiền?")).isFalse();
        assertThat(ChatAssistant.looksLikeConsent("Cho mình xem phòng khác")).isFalse();
        assertThat(ChatAssistant.looksLikeConsent("Mình cần 2 phòng cho 4 người"))
                .isFalse();
        assertThat(ChatAssistant.looksLikeConsent("")).isFalse();
    }

    @Test
    void aQuestionThatContainsAYesWordIsStillAQuestion() {
        // "được không" is asking, not agreeing - and the only thing consent unlocks is placing an order.
        assertThat(ChatAssistant.looksLikeConsent("Đổi sang ngày 20/12 được không"))
                .isFalse();
        assertThat(ChatAssistant.looksLikeConsent("Phòng nào cũng được chứ?")).isFalse();
        assertThat(ChatAssistant.looksLikeConsent("Ok vậy giá bao nhiêu?")).isFalse();
        assertThat(ChatAssistant.looksLikeConsent("Xác nhận giúp mình nhé")).isTrue();
    }

    @Test
    void aWordThatOnlyContainsAYesInsideItDoesNotCount() {
        // "oke" inside another word, and "u" inside a word, must not read as an agreement.
        assertThat(ChatAssistant.looksLikeConsent("okela gì đó")).isFalse();
        assertThat(ChatAssistant.looksLikeConsent("Có bún chả không")).isFalse();
    }
}
