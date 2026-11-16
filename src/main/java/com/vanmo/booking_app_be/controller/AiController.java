package com.vanmo.booking_app_be.controller;

import java.security.Principal;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.ai.AiClient;
import com.vanmo.booking_app_be.ai.AiSearchService;
import com.vanmo.booking_app_be.ai.AiTourSearch;
import com.vanmo.booking_app_be.ai.AiTripPlanner;
import com.vanmo.booking_app_be.ai.ChatAssistant;
import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.AiBranchSettingsRequest;
import com.vanmo.booking_app_be.dto.request.AiSearchRequest;
import com.vanmo.booking_app_be.dto.response.AiBranchSettingsResponse;
import com.vanmo.booking_app_be.dto.response.AiSearchResponse;
import com.vanmo.booking_app_be.dto.response.AiSuggestionResponse;
import com.vanmo.booking_app_be.dto.response.AiTourSearchResponse;
import com.vanmo.booking_app_be.dto.response.AiTripPlanResponse;

import lombok.RequiredArgsConstructor;

/**
 * AI features (Google Gemini, Claude or another OpenAI-compatible model - see {@link AiClient}). Every call goes
 * through the backend - the app never holds the API key.
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiClient aiClient;
    private final AiSearchService aiSearchService;
    private final ChatAssistant chatAssistant;
    private final AiTourSearch aiTourSearch;
    private final AiTripPlanner aiTripPlanner;

    /** Public: lets the app hide AI buttons when the server has no API key. */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of(
                "enabled",
                aiClient.isEnabled(),
                "provider",
                aiClient.getProvider().name().toLowerCase()));
    }

    /** Public like the rest of browsing; rate-limited per account, or per IP when signed out. */
    @PostMapping("/search")
    public ApiResponse<AiSearchResponse> search(
            @Valid @RequestBody AiSearchRequest req, Principal principal, HttpServletRequest http) {
        String client = principal != null ? "user:" + principal.getName() : "ip:" + http.getRemoteAddr();
        return ApiResponse.ok(aiSearchService.search(req.getQuery(), client));
    }

    /** Public too: "tour biển Hạ Long 2 ngày dưới 2 triệu" → the tours the backend matched. */
    @PostMapping("/tours/search")
    public ApiResponse<AiTourSearchResponse> searchTours(
            @Valid @RequestBody AiSearchRequest req, Principal principal, HttpServletRequest http) {
        String client = principal != null ? "user:" + principal.getName() : "ip:" + http.getRemoteAddr();
        return ApiResponse.ok(aiTourSearch.search(req.getQuery(), client));
    }

    /** Public: a day-by-day plan built from branches with free rooms and running tours. */
    @PostMapping("/trip-plan")
    public ApiResponse<AiTripPlanResponse> planTrip(
            @Valid @RequestBody AiSearchRequest req, Principal principal, HttpServletRequest http) {
        String client = principal != null ? "user:" + principal.getName() : "ip:" + http.getRemoteAddr();
        return ApiResponse.ok(aiTripPlanner.plan(req.getQuery(), client));
    }

    /** Branch team: a draft reply to review and send (nothing reaches the guest). */
    @PostMapping("/conversations/{id}/suggest-reply")
    public ApiResponse<AiSuggestionResponse> suggestReply(@PathVariable Long id) {
        return ApiResponse.ok(chatAssistant.suggest(id));
    }

    @GetMapping("/branches/{hotelId}/settings")
    public ApiResponse<AiBranchSettingsResponse> settings(@PathVariable Long hotelId) {
        return ApiResponse.ok(chatAssistant.settings(hotelId));
    }

    /** Owner or branch manager turns the assistant's auto-reply on or off. */
    @PutMapping("/branches/{hotelId}/settings")
    public ApiResponse<AiBranchSettingsResponse> updateSettings(
            @PathVariable Long hotelId, @Valid @RequestBody AiBranchSettingsRequest req) {
        return ApiResponse.ok(chatAssistant.updateSettings(hotelId, req));
    }
}
