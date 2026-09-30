package com.vanmo.booking_app_be.websocket;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import lombok.extern.slf4j.Slf4j;

/**
 * Open chat sockets per account (one account can be signed in on a phone and a browser at once). Sessions are wrapped
 * in {@link ConcurrentWebSocketSessionDecorator} because a message can be fanned out from several request threads at
 * the same time, and a raw session doesn't allow concurrent sends. Single-instance only: with more than one backend
 * node this would need a shared broker.
 */
@Slf4j
@Component
public class ChatSessionRegistry {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final Map<String, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void register(String accountId, WebSocketSession session) {
        sessions.computeIfAbsent(accountId, id -> new ConcurrentHashMap<>())
                .put(
                        session.getId(),
                        new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
    }

    public void unregister(String accountId, WebSocketSession session) {
        sessions.computeIfPresent(accountId, (id, open) -> {
            open.remove(session.getId());
            return open.isEmpty() ? null : open;
        });
    }

    /** Has the app open right now - used to skip push notifications for people already looking at the chat. */
    public boolean isOnline(String accountId) {
        return accountId != null && sessions.containsKey(accountId);
    }

    public void send(Collection<String> accountIds, String payload) {
        for (String accountId : accountIds) {
            if (accountId != null) {
                sessions.getOrDefault(accountId, Map.of()).values().forEach(session -> sendTo(session, payload));
            }
        }
    }

    /** Reply on one socket (the heartbeat), through the same thread-safe wrapper. */
    public void reply(String accountId, WebSocketSession session, String payload) {
        WebSocketSession wrapped = sessions.getOrDefault(accountId, Map.of()).get(session.getId());
        if (wrapped != null) {
            sendTo(wrapped, payload);
        }
    }

    private void sendTo(WebSocketSession session, String payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException | IllegalStateException e) {
            log.debug("Dropping chat socket {}: {}", session.getId(), e.getMessage());
            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (IOException ignored) {
                // already gone
            }
        }
    }
}
