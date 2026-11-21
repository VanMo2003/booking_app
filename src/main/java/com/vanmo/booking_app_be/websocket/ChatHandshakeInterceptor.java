package com.vanmo.booking_app_be.websocket;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import com.vanmo.booking_app_be.config.CustomJwtDecoder;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

/**
 * Authenticates the WebSocket upgrade with the same access token as the REST API. Browsers can't set headers on a
 * WebSocket, so the token comes as {@code ?token=}; an {@code Authorization: Bearer} header works too. Tokens go
 * through {@link CustomJwtDecoder}, so logged-out tokens are refused like everywhere else. Admins have no chat.
 */
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ACCOUNT_ID = "accountId";

    private final CustomJwtDecoder jwtDecoder;
    private final AccountRepository accountRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String token = token(request);
        Account account = null;
        if (token != null) {
            try {
                Jwt jwt = jwtDecoder.decode(token);
                account = accountRepository.findByUsername(jwt.getSubject()).orElse(null);
            } catch (RuntimeException e) {
                account = null;
            }
        }
        if (account == null || account.getRole() == Role.ADMIN) {
            response.setStatusCode(account == null ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(ACCOUNT_ID, account.getId());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {}

    private static String token(ServerHttpRequest request) {
        String query = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (query != null && !query.isBlank()) {
            return query;
        }
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
