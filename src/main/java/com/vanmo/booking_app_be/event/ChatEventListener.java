package com.vanmo.booking_app_be.event;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.ai.ChatAssistant;
import com.vanmo.booking_app_be.dto.response.ConversationResponse;
import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.utils.AppLinks;
import com.vanmo.booking_app_be.utils.NotificationMessages;
import com.vanmo.booking_app_be.websocket.ChatSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Delivers chat changes once they're committed: every open socket of both sides gets the event (so a second device
 * or another staff member sees it too), and the other side's members without an open socket get a push. Runs on the
 * request thread (not {@code @Async}) so two quick messages reach sockets in the order they were sent; the push
 * itself is async ({@link PushNotificationListener}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final ChatSessionRegistry registry;
    private final ObjectMapper objectMapper;
    private final PushNotificationListener pushNotificationListener;
    private final ChatAssistant chatAssistant;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChatUpdated(ChatUpdatedEvent event) {
        List<String> customer = event.customerAccountId() == null ? List.of() : List.of(event.customerAccountId());
        registry.send(customer, payload(event, event.customerView()));
        registry.send(event.hotelAccountIds(), payload(event, event.hotelView()));

        if (event.type() == ChatUpdatedEvent.Type.MESSAGE) {
            push(event);
            if (event.message().getSenderSide() == ChatSide.CUSTOMER) {
                // Answers later only if the branch turned auto-reply on and nobody replied in the meantime.
                chatAssistant.onCustomerMessage(
                        event.customerView().getId(), event.message().getId());
            }
        }
    }

    private void push(ChatUpdatedEvent event) {
        boolean fromCustomer = event.message().getSenderSide() == ChatSide.CUSTOMER;
        List<String> recipients = (fromCustomer ? event.hotelAccountIds() : customerIds(event))
                .stream()
                        .filter(id -> !id.equals(event.senderAccountId()) && !registry.isOnline(id))
                        .toList();
        if (recipients.isEmpty()) {
            return;
        }
        ConversationResponse conversation = event.customerView();
        String content = event.message().getContent();
        NotificationMessages.Message message = fromCustomer
                ? NotificationMessages.chatToHotel(conversation.getCustomerName(), conversation.getHotelName(), content)
                : NotificationMessages.chatToCustomer(conversation.getHotelName(), content);

        Map<String, String> data = new HashMap<>();
        data.put("type", "CHAT_MESSAGE");
        data.put("link", AppLinks.chat(conversation.getId()));
        data.put("referenceId", conversation.getId().toString());
        // One notification per conversation on Android: a newer message replaces the older one.
        data.put("tag", "chat-" + conversation.getId());
        // Called directly rather than published: an event published from an after-commit callback would register
        // on a transaction that is already finishing and never fire. The call still goes through the @Async proxy.
        pushNotificationListener.onPushRequested(
                new PushRequestedEvent(recipients, message.title(), message.body(), data));
    }

    private static List<String> customerIds(ChatUpdatedEvent event) {
        return event.customerAccountId() == null ? List.of() : List.of(event.customerAccountId());
    }

    private String payload(ChatUpdatedEvent event, ConversationResponse view) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", event.type().name());
        body.put("conversation", view);
        if (event.message() != null) {
            body.put("message", event.message());
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize chat event", e);
            return "{\"type\":\"" + event.type().name() + "\"}";
        }
    }
}
