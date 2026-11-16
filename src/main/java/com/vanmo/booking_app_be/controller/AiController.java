package com.vanmo.booking_app_be.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.ai.AiClient;
import com.vanmo.booking_app_be.ai.AiContentAudit;
import com.vanmo.booking_app_be.ai.AiDailyBrief;
import com.vanmo.booking_app_be.ai.AiGuestAssistant;
import com.vanmo.booking_app_be.ai.AiPricingAdvisor;
import com.vanmo.booking_app_be.ai.AiReportAnalyst;
import com.vanmo.booking_app_be.ai.AiReviewThemes;
import com.vanmo.booking_app_be.ai.AiSearchService;
import com.vanmo.booking_app_be.ai.AiTourSearch;
import com.vanmo.booking_app_be.ai.AiTripPlanner;
import com.vanmo.booking_app_be.ai.ChatAssistant;
import com.vanmo.booking_app_be.ai.ChatSummarizer;
import com.vanmo.booking_app_be.ai.ReviewDigest;
import com.vanmo.booking_app_be.ai.TourDraftWriter;
import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.AiBranchSettingsRequest;
import com.vanmo.booking_app_be.dto.request.AiReportQuestionRequest;
import com.vanmo.booking_app_be.dto.request.AiSearchRequest;
import com.vanmo.booking_app_be.dto.request.AiTourDraftRequest;
import com.vanmo.booking_app_be.dto.response.AiBranchSettingsResponse;
import com.vanmo.booking_app_be.dto.response.AiContentAuditResponse;
import com.vanmo.booking_app_be.dto.response.AiDailyBriefResponse;
import com.vanmo.booking_app_be.dto.response.AiGuestAnswerResponse;
import com.vanmo.booking_app_be.dto.response.AiPricingAdviceResponse;
import com.vanmo.booking_app_be.dto.response.AiReportAnswerResponse;
import com.vanmo.booking_app_be.dto.response.AiReviewThemesResponse;
import com.vanmo.booking_app_be.dto.response.AiSearchResponse;
import com.vanmo.booking_app_be.dto.response.AiSuggestionResponse;
import com.vanmo.booking_app_be.dto.response.AiTourDraftResponse;
import com.vanmo.booking_app_be.dto.response.AiTourSearchResponse;
import com.vanmo.booking_app_be.dto.response.AiTripPlanResponse;
import com.vanmo.booking_app_be.dto.response.ChatSummaryResponse;
import com.vanmo.booking_app_be.dto.response.ReviewDigestResponse;

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
    private final ReviewDigest reviewDigest;
    private final TourDraftWriter tourDraftWriter;
    private final AiReportAnalyst aiReportAnalyst;
    private final AiPricingAdvisor aiPricingAdvisor;
    private final AiGuestAssistant aiGuestAssistant;
    private final ChatSummarizer chatSummarizer;
    private final AiReviewThemes aiReviewThemes;
    private final AiContentAudit aiContentAudit;
    private final AiDailyBrief aiDailyBrief;

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

    /**
     * Public: what guests keep saying about a branch, read out of its own reviews. Cached per branch, so opening a
     * hotel page does not call the model again.
     */
    @GetMapping("/hotels/{hotelId}/review-digest")
    public ApiResponse<ReviewDigestResponse> reviewDigest(@PathVariable Long hotelId) {
        return ApiResponse.ok(reviewDigest.of(hotelId));
    }

    /** Branch owner or manager: the long fields of a tour drafted from a few words, to edit and save. */
    @PostMapping("/tours/draft")
    public ApiResponse<AiTourDraftResponse> draftTour(@Valid @RequestBody AiTourDraftRequest req) {
        return ApiResponse.ok(tourDraftWriter.draft(req));
    }

    /**
     * Branch manager (hotelId) or chain owner (hotelChainId): a question about the numbers, answered from the same
     * report figures the report screens show - the access checks are the report service's own.
     */
    @PostMapping("/reports/ask")
    public ApiResponse<AiReportAnswerResponse> askReports(@Valid @RequestBody AiReportQuestionRequest req) {
        return ApiResponse.ok(aiReportAnalyst.ask(req));
    }

    /**
     * Branch manager or above: what to do with the branch's room prices, per room type, from its own figures. The
     * report service's guards apply; nothing changes a price.
     */
    @GetMapping("/branches/{hotelId}/pricing-advice")
    public ApiResponse<AiPricingAdviceResponse> pricingAdvice(@PathVariable Long hotelId) {
        return ApiResponse.ok(aiPricingAdvisor.advise(hotelId));
    }

    /** A guest asking about their own trips; it reads only their own bookings, orders and vouchers. */
    @PostMapping("/assistant/ask")
    public ApiResponse<AiGuestAnswerResponse> askAssistant(@Valid @RequestBody AiSearchRequest req) {
        return ApiResponse.ok(aiGuestAssistant.ask(req.getQuery()));
    }

    /** Branch team: a draft reply to review and send (nothing reaches the guest). */
    @PostMapping("/conversations/{id}/suggest-reply")
    public ApiResponse<AiSuggestionResponse> suggestReply(@PathVariable Long id) {
        return ApiResponse.ok(chatAssistant.suggest(id));
    }

    /**
     * Branch manager or above: how ready the branch's page is to sell. The checks and the score are counted from
     * its own rows; only the advice is written by the assistant.
     */
    @GetMapping("/branches/{hotelId}/content-audit")
    public ApiResponse<AiContentAuditResponse> contentAudit(@PathVariable Long hotelId) {
        return ApiResponse.ok(aiContentAudit.of(hotelId));
    }

    /**
     * Branch manager or above: what the branch has to deal with today. Every figure is counted from its own rows;
     * the assistant only puts them in order and says what to do first.
     */
    @GetMapping("/branches/{hotelId}/daily-brief")
    public ApiResponse<AiDailyBriefResponse> dailyBrief(@PathVariable Long hotelId) {
        return ApiResponse.ok(aiDailyBrief.of(hotelId));
    }

    /** Chain owner: what guests across the chain keep talking about in a period, topic by topic. */
    @GetMapping("/chains/{hotelChainId}/review-themes")
    public ApiResponse<AiReviewThemesResponse> reviewThemes(
            @PathVariable Long hotelChainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(aiReviewThemes.of(hotelChainId, from, to));
    }

    /** Branch team: a long thread in one note - what the guest wants, what is open, how urgent. */
    @GetMapping("/conversations/{id}/summary")
    public ApiResponse<ChatSummaryResponse> chatSummary(@PathVariable Long id) {
        return ApiResponse.ok(chatSummarizer.of(id));
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
