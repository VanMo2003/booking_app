package com.vanmo.booking_app_be.event;

import java.util.List;

import com.vanmo.booking_app_be.dto.response.ChatMessageResponse;
import com.vanmo.booking_app_be.dto.response.ConversationResponse;

/**
 * A conversation changed (new message, or one side read it). Built inside the transaction with everything the
 * WebSocket fan-out and the push need, so the after-commit listener never touches lazy entities.
 *
 * @param message null for {@link Type#READ}
 * @param customerAccountId null if the guest's profile has no login account
 * @param senderAccountId the account that caused the change (never pushed to); null for the chat assistant
 */
public record ChatUpdatedEvent(
        Type type,
        ChatMessageResponse message,
        ConversationResponse customerView,
        ConversationResponse hotelView,
        String customerAccountId,
        List<String> hotelAccountIds,
        String senderAccountId) {

    public enum Type {
        MESSAGE,
        READ
    }
}
