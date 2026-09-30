package com.vanmo.booking_app_be.ai;

import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * One entry point for every model call, whichever provider is configured. The AI services always build requests
 * and read responses in the Anthropic Messages shape (system blocks, {@code tool_use}/{@code tool_result}); for
 * OpenAI-compatible providers - Google Gemini's compatibility endpoint, Groq, OpenRouter, Ollama - {@link
 * OpenAiCompat} translates on the way out and back. Plain {@link RestClient}, no SDK, so the JSON stays visible and
 * {@code AI_BASE_URL} can point at a local stub.
 *
 * <p>Provider: {@code AI_PROVIDER} = {@code anthropic} | {@code gemini} | {@code openai}; when unset it follows the
 * key that is present ({@code GEMINI_API_KEY} → gemini, {@code ANTHROPIC_API_KEY} → anthropic). Without any key
 * every AI feature is off ({@link #isEnabled()}), the same way push is off without Firebase.
 */
@Slf4j
@Component
public class AiClient {

    public enum Provider {
        ANTHROPIC("https://api.anthropic.com", "claude-sonnet-5", "claude-haiku-4-5-20251001"),
        // Both have a free tier in Google AI Studio.
        GEMINI("https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.8-flash", "gemini-3.5-flash-lite"),
        // Any other Chat Completions server: set AI_BASE_URL and the models.
        OPENAI("https://api.openai.com/v1", "", "");

        final String baseUrl;
        final String assistantModel;
        final String fastModel;

        Provider(String baseUrl, String assistantModel, String fastModel) {
            this.baseUrl = baseUrl;
            this.assistantModel = assistantModel;
            this.fastModel = fastModel;
        }
    }

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;

    @Getter
    private final Provider provider;

    @Getter
    private final String assistantModel;

    @Getter
    private final String fastModel;

    public AiClient(
            ObjectMapper objectMapper,
            @Value("${ai.provider:}") String provider,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.gemini-api-key:}") String geminiApiKey,
            @Value("${ai.anthropic-api-key:}") String anthropicApiKey,
            @Value("${ai.base-url:}") String baseUrl,
            @Value("${ai.assistant-model:}") String assistantModel,
            @Value("${ai.fast-model:}") String fastModel,
            @Value("${ai.timeout-seconds:60}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.provider = resolveProvider(provider, geminiApiKey, anthropicApiKey);
        String providerKey =
                switch (this.provider) {
                    case GEMINI -> geminiApiKey;
                    case ANTHROPIC -> anthropicApiKey;
                    case OPENAI -> "";
                };
        this.apiKey = firstNonBlank(apiKey, providerKey);
        this.assistantModel = firstNonBlank(assistantModel, this.provider.assistantModel);
        this.fastModel = firstNonBlank(fastModel, this.provider.fastModel, this.assistantModel);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder()
                .baseUrl(stripSlash(firstNonBlank(baseUrl, this.provider.baseUrl)))
                .requestFactory(factory)
                .build();

        if (this.apiKey.isEmpty()) {
            log.info("No AI API key (GEMINI_API_KEY / ANTHROPIC_API_KEY / AI_API_KEY) - AI features are off");
        } else if (this.assistantModel.isEmpty()) {
            log.warn("AI_PROVIDER=openai needs AI_ASSISTANT_MODEL - AI features are off");
        } else {
            log.info(
                    "AI features on: provider={}, assistant={}, fast={}",
                    this.provider,
                    this.assistantModel,
                    this.fastModel);
        }
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty() && !assistantModel.isEmpty();
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

    /**
     * Sends one request (Anthropic Messages shape) and returns the answer in the same shape, whatever the provider.
     * Any failure becomes {@link ErrorCode#AI_UNAVAILABLE}.
     */
    public JsonNode createMessage(ObjectNode request) {
        if (!isEnabled()) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        try {
            JsonNode response = provider == Provider.ANTHROPIC ? anthropic(request) : openAiCompatible(request);
            if (response == null
                    || !response.path("content").isArray()
                    || "error".equals(response.path("stop_reason").asText())) {
                throw new AppException(ErrorCode.AI_UNAVAILABLE);
            }
            return response;
        } catch (RestClientResponseException e) {
            log.warn("{} rejected a request ({}): {}", provider, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        } catch (RestClientException | JsonProcessingException e) {
            log.warn("Cannot reach {}: {}", provider, e.getMessage());
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
    }

    private JsonNode anthropic(ObjectNode request) throws JsonProcessingException {
        return restClient
                .post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(request))
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode openAiCompatible(ObjectNode request) throws JsonProcessingException {
        ObjectNode chat = OpenAiCompat.toChatRequest(request, objectMapper);
        JsonNode response = restClient
                .post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(chat))
                .retrieve()
                .body(JsonNode.class);
        return response == null ? null : OpenAiCompat.toMessage(response, objectMapper);
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

    /**
     * Fallback when a model answered a forced tool call with plain JSON text instead (some compatible servers ignore
     * {@code tool_choice}): the first {@code {...}} object in the text, or null.
     */
    public JsonNode jsonObjectIn(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(text.substring(start, end + 1));
            return node.isObject() ? node : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** A {@code {"type":"text","text":...}} block, optionally marked for prompt caching (Anthropic only). */
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

    /** Explicit AI_PROVIDER wins; otherwise Claude only when its key alone is set, else the free Gemini default. */
    private static Provider resolveProvider(String configured, String geminiKey, String anthropicKey) {
        if (configured != null && !configured.isBlank()) {
            try {
                return Provider.valueOf(configured.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown AI_PROVIDER '{}' - expected anthropic, gemini or openai", configured);
            }
        }
        boolean onlyAnthropic =
                (geminiKey == null || geminiKey.isBlank()) && anthropicKey != null && !anthropicKey.isBlank();
        return onlyAnthropic ? Provider.ANTHROPIC : Provider.GEMINI;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "";
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
