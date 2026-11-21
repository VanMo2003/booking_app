package com.vanmo.booking_app_be.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.RequiredArgsConstructor;

/**
 * {@code /ws/chat}: server → client only. Events are JSON objects with a {@code type}:
 * <ul>
 *   <li>{@code CONNECTED} - right after the upgrade;
 *   <li>{@code MESSAGE} - {@code conversation} (as the receiver sees it) + {@code message};
 *   <li>{@code READ} - {@code conversation} after one side opened it;
 *   <li>{@code PONG} - answer to a client {@code {"type":"PING"}} heartbeat.
 * </ul>
 * Messages are sent with {@code POST /conversations/{id}/messages}, not over the socket, so validation, authorization
 * and error replies stay in the normal REST path.
 */
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatSessionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String accountId = accountId(session);
        registry.register(accountId, session);
        registry.reply(accountId, session, "{\"type\":\"CONNECTED\"}");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (message.getPayload().contains("\"PING\"")) {
            registry.reply(accountId(session), session, "{\"type\":\"PONG\"}");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregister(accountId(session), session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        registry.unregister(accountId(session), session);
    }

    private static String accountId(WebSocketSession session) {
        return (String) session.getAttributes().get(ChatHandshakeInterceptor.ACCOUNT_ID);
    }
}
