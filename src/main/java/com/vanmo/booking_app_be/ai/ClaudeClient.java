package com.vanmo.booking_app_be.ai;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin client for the Claude Messages API ({@code POST /v1/messages}) on Spring's {@link RestClient} - no SDK, so the
 * request JSON stays visible and {@code ANTHROPIC_BASE_URL} can point at a local stub for tests. Without
 * {@code ANTHROPIC_API_KEY} every AI feature is off ({@link #isEnabled()}), the same way push is off without Firebase.
 */
@Slf4j
@Component
public class ClaudeClient {

    private static final String API_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;

    @Getter
    private final String assistantModel;

    @Getter
    private final String fastModel;

    public ClaudeClient(
            ObjectMapper objectMapper,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${ai.assistant-model:claude-sonnet-5}") String assistantModel,
            @Value("${ai.fast-model:claude-haiku-4-5-20251001}") String fastModel,
            @Value("${ai.timeout-seconds:60}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.assistantModel = assistantModel;
        this.fastModel = fastModel;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient =
                RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        if (!isEnabled()) {
            log.info("ANTHROPIC_API_KEY is not set - AI features (chat assistant, natural-language search) are off");
        }
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    public ObjectNode newRequest(String model, int maxTokens) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("max_tokens", maxTokens);
        request.putArray("messages");
        return request;
    }

    public ObjectMapper json() {
        return objectMapper;
    }

    /** Sends one Messages API request; any failure becomes {@link ErrorCode#AI_UNAVAILABLE}. */
    public JsonNode createMessage(ObjectNode request) {
        if (!isEnabled()) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        try {
            JsonNode response = restClient
                    .post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(request))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("content").isArray()) {
                throw new AppException(ErrorCode.AI_UNAVAILABLE);
            }
            return response;
        } catch (RestClientResponseException e) {
            log.warn("Claude API rejected a request ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Cannot reach the Claude API: {}", e.getMessage());
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
    }

    /** Joins the text blocks of a response. */
    public static String text(JsonNode response) {
        StringBuilder text = new StringBuilder();
        for (JsonNode block : response.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.toString().strip();
    }

    /** First {@code tool_use} block calling {@code name}, or null. */
    public static JsonNode toolUse(JsonNode response, String name) {
        for (JsonNode block : response.path("content")) {
            if ("tool_use".equals(block.path("type").asText())
                    && name.equals(block.path("name").asText())) {
                return block;
            }
        }
        return null;
    }

    /** A {@code {"type":"text","text":...}} block, optionally marked for prompt caching. */
    public ObjectNode textBlock(String text, boolean cache) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", "text");
        block.put("text", text);
        if (cache) {
            block.putObject("cache_control").put("type", "ephemeral");
        }
        return block;
    }

    public ObjectNode userMessage(String text) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", text);
        return message;
    }

    public ArrayNode array() {
        return objectMapper.createArrayNode();
    }
}
