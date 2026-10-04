package com.vanmo.booking_app_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.vanmo.booking_app_be.websocket.ChatHandshakeInterceptor;
import com.vanmo.booking_app_be.websocket.ChatWebSocketHandler;

import lombok.RequiredArgsConstructor;

/** Plain WebSocket (no STOMP) at {@code <context-path>/ws/chat?token=<access token>}. */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeInterceptor chatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // The access token authenticates the socket (no cookies), so any origin may connect - same as the REST CORS.
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }

    /**
     * Closes a socket that sent nothing for 70 s (the app pings every 25 s). A phone that lost its network or was
     * killed never sends a close frame; without this its socket stays "online" and chat pushes are skipped for it.
     */
    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(70_000L);
        return container;
    }
}
