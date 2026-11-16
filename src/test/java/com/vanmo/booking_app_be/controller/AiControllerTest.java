package com.vanmo.booking_app_be.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.vanmo.booking_app_be.ai.AiClient;
import com.vanmo.booking_app_be.ai.AiContentAudit;
import com.vanmo.booking_app_be.ai.AiDailyBrief;
import com.vanmo.booking_app_be.ai.AiGuestAssistant;
import com.vanmo.booking_app_be.ai.AiHotelQa;
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
import com.vanmo.booking_app_be.dto.request.AiTourDraftRequest;
import com.vanmo.booking_app_be.dto.response.AiBranchSettingsResponse;
import com.vanmo.booking_app_be.dto.response.AiContentAuditResponse;
import com.vanmo.booking_app_be.dto.response.AiDailyBriefResponse;
import com.vanmo.booking_app_be.dto.response.AiGuestAnswerResponse;
import com.vanmo.booking_app_be.dto.response.AiHotelAnswerResponse;
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
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.exception.GlobalExceptionHandler;

/**
 * The HTTP side of every AI endpoint: the route exists, the body is validated, and the answer comes back inside
 * {@code ApiResponse} with the fields the app reads. The AI services themselves are mocked - what is tested here is
 * the wiring, which the services' own unit tests cannot see.
 */
class AiControllerTest {

    private final AiClient aiClient = mock(AiClient.class);
    private final AiSearchService aiSearchService = mock(AiSearchService.class);
    private final ChatAssistant chatAssistant = mock(ChatAssistant.class);
    private final AiTourSearch aiTourSearch = mock(AiTourSearch.class);
    private final AiTripPlanner aiTripPlanner = mock(AiTripPlanner.class);
    private final ReviewDigest reviewDigest = mock(ReviewDigest.class);
    private final TourDraftWriter tourDraftWriter = mock(TourDraftWriter.class);
    private final AiReportAnalyst aiReportAnalyst = mock(AiReportAnalyst.class);
    private final AiPricingAdvisor aiPricingAdvisor = mock(AiPricingAdvisor.class);
    private final AiGuestAssistant aiGuestAssistant = mock(AiGuestAssistant.class);
    private final ChatSummarizer chatSummarizer = mock(ChatSummarizer.class);
    private final AiReviewThemes aiReviewThemes = mock(AiReviewThemes.class);
    private final AiContentAudit aiContentAudit = mock(AiContentAudit.class);
    private final AiDailyBrief aiDailyBrief = mock(AiDailyBrief.class);
    private final AiHotelQa aiHotelQa = mock(AiHotelQa.class);

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        AiController controller = new AiController(
                aiClient,
                aiSearchService,
                chatAssistant,
                aiTourSearch,
                aiTripPlanner,
                reviewDigest,
                tourDraftWriter,
                aiReportAnalyst,
                aiPricingAdvisor,
                aiGuestAssistant,
                chatSummarizer,
                aiReviewThemes,
                aiContentAudit,
                aiDailyBrief,
                aiHotelQa);
        // Spring Boot's own Jackson defaults, so dates serialise the way the app really answers.
        com.fasterxml.jackson.databind.ObjectMapper json = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(json))
                .build();
    }

    @Test
    void statusTellsTheAppWhetherAiIsOn() throws Exception {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.getProvider()).thenReturn(AiClient.Provider.GEMINI);

        mvc.perform(get("/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.provider").value("gemini"));
    }

    @Test
    void tourSearchAnswersWithTheToursTheBackendMatched() throws Exception {
        when(aiTourSearch.search(anyString(), anyString()))
                .thenReturn(AiTourSearchResponse.builder()
                        .query("tour biển Hạ Long")
                        .summary("Tour biển Hạ Long")
                        .results(List.of(TourResponse.builder()
                                .id(7L)
                                .name("Du thuyền vịnh Hạ Long")
                                .salePrice(new BigDecimal("500000"))
                                .available(true)
                                .build()))
                        .notes(List.of())
                        .build());

        mvc.perform(post("/ai/tours/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"tour biển Hạ Long\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].id").value(7))
                .andExpect(jsonPath("$.data.results[0].salePrice").value(500000));
    }

    @Test
    void aBlankQueryIsRefusedBeforeTheModelIsCalled() throws Exception {
        mvc.perform(post("/ai/tours/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"   \"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/ai/trip-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/ai/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tripPlanCarriesTheStayAndTheCost() throws Exception {
        when(aiTripPlanner.plan(anyString(), anyString()))
                .thenReturn(AiTripPlanResponse.builder()
                        .query("Hạ Long 3 ngày")
                        .nights(2)
                        .adults(2)
                        .stay(AiTripPlanResponse.Stay.builder()
                                .hotelId(6L)
                                .hotelName("Hạ Long Bay Hotel")
                                .roomNumber("101")
                                .pricePerNight(new BigDecimal("900000"))
                                .checkin(LocalDate.of(2026, 12, 5))
                                .checkout(LocalDate.of(2026, 12, 7))
                                .build())
                        .cost(AiTripPlanResponse.Cost.builder()
                                .rooms(new BigDecimal("1800000"))
                                .total(new BigDecimal("1800000"))
                                .build())
                        .days(List.of())
                        .tours(List.of())
                        .build());

        mvc.perform(post("/ai/trip-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Hạ Long 3 ngày\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stay.hotelName").value("Hạ Long Bay Hotel"))
                .andExpect(jsonPath("$.data.stay.checkout").value("2026-12-07"))
                .andExpect(jsonPath("$.data.cost.total").value(1800000));
    }

    @Test
    void theGuestAssistantAnswersWithItsLinks() throws Exception {
        when(aiGuestAssistant.ask(anyString()))
                .thenReturn(AiGuestAnswerResponse.builder()
                        .question("Đơn của tôi?")
                        .answer("Bạn có đơn phòng #26.")
                        .links(List.of(AiGuestAnswerResponse.Link.builder()
                                .label("Xem đơn phòng #26")
                                .target("bookings/26")
                                .build()))
                        .build());

        mvc.perform(post("/ai/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Đơn của tôi?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.links[0].target").value("bookings/26"));
    }

    @Test
    void reviewDigestAndThemesAreReachableWithTheirIds() throws Exception {
        when(reviewDigest.of(6L))
                .thenReturn(ReviewDigestResponse.builder()
                        .hotelId(6L)
                        .available(true)
                        .summary("Khách thích vị trí.")
                        .pros(List.of("Vị trí ngay phố cổ"))
                        .reviewCount(12)
                        .averageRating(4.3)
                        .ratingCounts(List.of(0, 1, 1, 4, 6))
                        .build());
        when(aiReviewThemes.of(eq(10L), any(), any()))
                .thenReturn(AiReviewThemesResponse.builder()
                        .hotelChainId(10L)
                        .reviewCount(24)
                        .themes(List.of(AiReviewThemesResponse.Theme.builder()
                                .topic("Vị trí")
                                .sentiment("GOOD")
                                .note("Gần phố cổ.")
                                .branches(List.of("Hoàn Kiếm Palace"))
                                .build()))
                        .build());

        mvc.perform(get("/ai/hotels/6/review-digest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.ratingCounts.length()").value(5));

        mvc.perform(get("/ai/chains/10/review-themes")
                        .param("from", "2026-08-19")
                        .param("to", "2026-11-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.themes[0].sentiment").value("GOOD"));
        verify(aiReviewThemes).of(10L, LocalDate.of(2026, 8, 19), LocalDate.of(2026, 11, 16));
    }

    @Test
    void reviewThemesNeedBothDates() throws Exception {
        mvc.perform(get("/ai/chains/10/review-themes").param("from", "2026-08-19"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theTourDraftNeedsABranchAndAName() throws Exception {
        when(tourDraftWriter.draft(any(AiTourDraftRequest.class)))
                .thenReturn(AiTourDraftResponse.builder()
                        .description("Dạo phố cổ buổi tối.")
                        .includes("Hướng dẫn viên, 8 món")
                        .itinerary(List.of())
                        .build());

        mvc.perform(post("/ai/tours/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\": 6, \"name\": \"Food tour phố cổ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Dạo phố cổ buổi tối."));

        mvc.perform(post("/ai/tours/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Thiếu mã cơ sở\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theReportEndpointsAnswerForABranchAndAChain() throws Exception {
        when(aiReportAnalyst.ask(any()))
                .thenReturn(AiReportAnswerResponse.builder()
                        .question("Doanh thu tháng này?")
                        .scope("chain")
                        .from(LocalDate.of(2026, 11, 1))
                        .to(LocalDate.of(2026, 11, 16))
                        .answer("Doanh thu tăng 20%.")
                        .build());
        when(aiPricingAdvisor.advise(6L))
                .thenReturn(AiPricingAdviceResponse.builder()
                        .hotelId(6L)
                        .items(List.of(AiPricingAdviceResponse.Item.builder()
                                .roomTypeName("Deluxe")
                                .action("RAISE")
                                .percent(8)
                                .suggestedPrice(new BigDecimal("972000"))
                                .build()))
                        .build());
        when(aiContentAudit.of(6L))
                .thenReturn(AiContentAuditResponse.builder()
                        .hotelId(6L)
                        .score(73)
                        .checks(List.of(AiContentAuditResponse.Check.builder()
                                .key("rooms_with_photo")
                                .label("Mọi phòng đều có ảnh")
                                .passed(false)
                                .done(3)
                                .total(5)
                                .build()))
                        .items(List.of())
                        .build());

        mvc.perform(post("/ai/reports/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Doanh thu tháng này?\", \"hotelChainId\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("chain"))
                .andExpect(jsonPath("$.data.from").value("2026-11-01"));

        mvc.perform(get("/ai/branches/6/pricing-advice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].action").value("RAISE"))
                .andExpect(jsonPath("$.data.items[0].suggestedPrice").value(972000));

        mvc.perform(get("/ai/branches/6/content-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(73))
                .andExpect(jsonPath("$.data.checks[0].passed").value(false));
    }

    @Test
    void askingAboutOneBranchComesBackWithItsLinks() throws Exception {
        when(aiHotelQa.ask(eq(6L), eq("Có tour ăn uống không?"), anyString()))
                .thenReturn(AiHotelAnswerResponse.builder()
                        .hotelId(6L)
                        .question("Có tour ăn uống không?")
                        .answer("Dạ cơ sở mình có Food tour phố cổ.")
                        .answered(true)
                        .links(List.of(AiHotelAnswerResponse.Link.builder()
                                .label("Xem tour Food tour phố cổ")
                                .target("tours/7")
                                .build()))
                        .build());

        mvc.perform(post("/ai/hotels/6/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"Có tour ăn uống không?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answered").value(true))
                .andExpect(jsonPath("$.data.links[0].target").value("tours/7"));
    }

    @Test
    void askingAboutOneBranchWithABlankQuestionIsRefused() throws Exception {
        mvc.perform(post("/ai/hotels/6/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dailyBriefComesBackWithItsSignalsAndTasks() throws Exception {
        when(aiDailyBrief.of(6L))
                .thenReturn(AiDailyBriefResponse.builder()
                        .hotelId(6L)
                        .date(LocalDate.of(2026, 11, 16))
                        .headline("Hôm nay có hai đơn treo cần gọi lại.")
                        .signals(List.of(AiDailyBriefResponse.Signal.builder()
                                .key("pending_bookings")
                                .label("Đơn phòng chờ xác nhận")
                                .value("2 đơn")
                                .level("URGENT")
                                .build()))
                        .tasks(List.of(AiDailyBriefResponse.Task.builder()
                                .title("Xác nhận đơn đang treo")
                                .priority("HIGH")
                                .how("Mở danh sách đơn chờ và gọi cho khách.")
                                .signalKey("pending_bookings")
                                .build()))
                        .build());

        mvc.perform(get("/ai/branches/6/daily-brief"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-11-16"))
                .andExpect(jsonPath("$.data.signals[0].level").value("URGENT"))
                .andExpect(jsonPath("$.data.tasks[0].signalKey").value("pending_bookings"));
    }

    @Test
    void theChatEndpointsAreReachable() throws Exception {
        when(chatSummarizer.of(12L))
                .thenReturn(ChatSummaryResponse.builder()
                        .conversationId(12L)
                        .urgency("HIGH")
                        .waitingOnHotel(true)
                        .messageCount(14)
                        .build());
        when(chatAssistant.suggest(12L))
                .thenReturn(AiSuggestionResponse.builder()
                        .content("Dạ em kiểm tra giúp anh.")
                        .build());
        when(chatAssistant.settings(6L))
                .thenReturn(AiBranchSettingsResponse.builder()
                        .hotelId(6L)
                        .autoReply(false)
                        .aiEnabled(true)
                        .build());
        when(chatAssistant.updateSettings(eq(6L), any()))
                .thenReturn(AiBranchSettingsResponse.builder()
                        .hotelId(6L)
                        .autoReply(true)
                        .aiEnabled(true)
                        .build());

        mvc.perform(get("/ai/conversations/12/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.urgency").value("HIGH"))
                .andExpect(jsonPath("$.data.waitingOnHotel").value(true));

        mvc.perform(post("/ai/conversations/12/suggest-reply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Dạ em kiểm tra giúp anh."));

        mvc.perform(get("/ai/branches/6/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoReply").value(false));

        mvc.perform(put("/ai/branches/6/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoReply\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoReply").value(true));
    }

    @Test
    void theRoomSearchStillAnswersTheWayTheAppExpects() throws Exception {
        when(aiSearchService.search(anyString(), anyString()))
                .thenReturn(AiSearchResponse.builder()
                        .query("phòng 2 người")
                        .results(List.of(AiSearchResponse.Result.builder()
                                .hotelId(6L)
                                .name("Hoàn Kiếm Palace")
                                .matchingRooms(3)
                                .fromPrice(new BigDecimal("850000"))
                                .build()))
                        .notes(List.of())
                        .build());

        mvc.perform(post("/ai/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"phòng 2 người\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].matchingRooms").value(3));
        verify(aiSearchService).search(eq("phòng 2 người"), anyString());
    }

    @Test
    void anIdThatIsNotANumberIsABadRequestNotAServerError() throws Exception {
        mvc.perform(get("/ai/branches/abc/pricing-advice")).andExpect(status().isBadRequest());
        verify(aiPricingAdvisor, org.mockito.Mockito.never()).advise(anyLong());
    }
}
