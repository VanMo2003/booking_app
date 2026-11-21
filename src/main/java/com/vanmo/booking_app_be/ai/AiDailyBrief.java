package com.vanmo.booking_app_be.ai;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vanmo.booking_app_be.dto.response.AiDailyBriefResponse;
import com.vanmo.booking_app_be.dto.response.FeedbackRow;
import com.vanmo.booking_app_be.dto.response.ReceivableDTO;
import com.vanmo.booking_app_be.dto.response.TargetProgressResponse;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.TourBooking;
import com.vanmo.booking_app_be.entity.enums.BookingStatus;
import com.vanmo.booking_app_be.entity.enums.RoomStatus;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.repository.BookingRepository;
import com.vanmo.booking_app_be.repository.ConversationRepository;
import com.vanmo.booking_app_be.repository.FeedbackRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.TourBookingRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.ReportService;

/**
 * "Việc hôm nay" for a branch: the arrivals and departures of the day, orders still waiting to be confirmed, tours
 * about to leave, guests waiting for an answer, bad reviews just in, money not collected, rooms out of service and
 * how the month's goal is going.
 *
 * <p>Every one of those is counted here from the branch's own rows, and each carries its own level
 * (URGENT / WATCH / QUIET) decided in code. The model is handed the finished list and only orders it and writes a
 * line each: what it costs to let this wait, and what to do. Advice about a quiet signal - or about something that
 * is not on the list at all - is dropped in {@link #build}, so the manager is never sent chasing a number nobody
 * counted. Without an API key the signals are still returned; only the wording is missing.
 */
@Service
public class AiDailyBrief {

    /** URGENT: something a person has to pick up today. */
    static final String URGENT = "URGENT";

    /** WATCH: worth a look, not on fire. */
    static final String WATCH = "WATCH";

    /** QUIET: nothing to do - shown so the manager sees it was checked. */
    static final String QUIET = "QUIET";

    /** An order left pending longer than this is the guest waiting, not the desk working. */
    static final int STALE_PENDING_HOURS = 24;

    /** How far back a bad review still counts as "just in". */
    static final int REVIEW_DAYS = 7;

    /** A review at or below this many stars is a complaint, not a rating. */
    static final int LOW_RATING = 2;

    /** Money uncollected this many days after check-out stops being paperwork. */
    static final int RECEIVABLE_OVERDUE_DAYS = 7;

    /** Being this many points of the month behind the calendar is a warning, not noise. */
    static final double TARGET_GAP_POINTS = 15;

    private static final String SYSTEM =
            """
			Bạn là trợ lý của quản lý một cơ sở lưu trú, viết bản tin đầu ngày. Hãy gọi set_daily_brief.
			Quy tắc:
			- Mục "SỐ LIỆU HÔM NAY" là sự thật hệ thống đã đếm. Không bịa thêm con số, không sửa con số đã cho, không nhắc việc không có trong danh sách.
			- tasks: chỉ về những dòng đang ở mức URGENT hoặc WATCH, tối đa 5 việc, việc cần làm sớm nhất đứng trước. Mỗi việc: title ngắn như một dòng việc cần làm; priority HIGH/MEDIUM/LOW; why một câu nói để lâu thì mất gì; how một câu việc cụ thể phải làm; signal_key đúng key của dòng đó.
			- Mọi dòng đều QUIET thì tasks để mảng rỗng và headline nói hôm nay nhẹ việc.
			- headline: 1-2 câu tiếng Việt, giọng đồng nghiệp nói chuyện buổi sáng, không khen sáo, không chào hỏi dài.
			- Văn bản thường, không markdown, không emoji.
			""";

    private static final String TOOL =
            """
			{
			"name": "set_daily_brief",
			"description": "Bản tin đầu ngày của cơ sở.",
			"input_schema": {
				"type": "object",
				"properties": {
				"headline": {"type": "string"},
				"tasks": {
					"type": "array",
					"items": {
					"type": "object",
					"properties": {
						"title": {"type": "string"},
						"priority": {"type": "string", "enum": ["HIGH", "MEDIUM", "LOW"]},
						"why": {"type": "string"},
						"how": {"type": "string"},
						"signal_key": {"type": "string"}
					},
					"required": ["title", "priority", "how", "signal_key"]
					}
				}
				},
				"required": ["headline", "tasks"]
			}
			}
			""";

    private static final List<BookingStatus> STAYING = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final List<TourBookingStatus> OPEN_TOURS =
            List.of(TourBookingStatus.PENDING, TourBookingStatus.CONFIRMED);

    private final AiClient ai;
    private final AiRateLimiter rateLimiter;
    private final AccessGuard accessGuard;
    private final HotelKnowledge knowledge;
    private final ReportService reportService;
    private final BookingRepository bookingRepository;
    private final TourBookingRepository tourBookingRepository;
    private final ConversationRepository conversationRepository;
    private final FeedbackRepository feedbackRepository;
    private final RoomRepository roomRepository;
    private final JsonNode tool;

    public AiDailyBrief(
            AiClient ai,
            AiRateLimiter rateLimiter,
            AccessGuard accessGuard,
            HotelKnowledge knowledge,
            ReportService reportService,
            BookingRepository bookingRepository,
            TourBookingRepository tourBookingRepository,
            ConversationRepository conversationRepository,
            FeedbackRepository feedbackRepository,
            RoomRepository roomRepository)
            throws JsonProcessingException {
        this.ai = ai;
        this.rateLimiter = rateLimiter;
        this.accessGuard = accessGuard;
        this.knowledge = knowledge;
        this.reportService = reportService;
        this.bookingRepository = bookingRepository;
        this.tourBookingRepository = tourBookingRepository;
        this.conversationRepository = conversationRepository;
        this.feedbackRepository = feedbackRepository;
        this.roomRepository = roomRepository;
        this.tool = ai.json().readTree(TOOL);
    }

    @Transactional(readOnly = true)
    public AiDailyBriefResponse of(Long hotelId) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        Hotel hotel = knowledge.hotel(hotelId);
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ChatAssistant.ZONE);
        LocalDate tomorrow = today.plusDays(1);

        // getTargetProgress and getReceivables run the report service's own guards again and return exactly what the
        // report screens show, so a brief can never disagree with them.
        TargetProgressResponse target =
                reportService.getTargetProgress(hotelId, YearMonth.from(today).toString());
        List<ReceivableDTO> receivables = reportService.getReceivables(hotelId);

        List<AiDailyBriefResponse.Signal> signals = signals(
                now,
                today,
                bookingRepository.findAllByHotel_IdAndCheckinDateAndBookingStatusIn(hotelId, today, STAYING),
                bookingRepository.findAllByHotel_IdAndCheckoutDateAndBookingStatusIn(hotelId, today, STAYING),
                bookingRepository.findAllByHotel_IdAndBookingStatus(hotelId, BookingStatus.PENDING),
                tourBookingRepository.findAllByHotel_IdAndTourDateInAndStatusIn(
                        hotelId, List.of(today, tomorrow), OPEN_TOURS),
                conversationRepository.countByHotel_IdInAndNeedsStaffTrue(List.of(hotelId)),
                conversationRepository.sumHotelUnread(List.of(hotelId)),
                feedbackRepository.findByHotel(hotelId),
                receivables,
                roomRepository.findAllByHotel_Id(hotelId),
                target);
        List<String> notes = new ArrayList<>();

        if (!ai.isEnabled()) {
            // The counting is the useful half and needs no model: hand it over without the wording.
            notes.add("Máy chủ chưa bật AI nên chưa có phần sắp xếp việc, nhưng số liệu vẫn đúng.");
            return AiDailyBriefResponse.builder()
                    .hotelId(hotelId)
                    .date(today)
                    .signals(signals)
                    .tasks(List.of())
                    .notes(notes)
                    .build();
        }
        rateLimiter.check("daily-brief:" + hotelId, 8, Duration.ofMinutes(10));
        JsonNode brief = ai.withModelFallback(ai.getAssistantModels(), model -> write(model, hotel, today, signals));
        return build(hotelId, today, signals, brief, notes);
    }

    /**
     * Every signal of the day, counted from the branch's rows. Each one is always present, quiet or not, so the
     * manager can tell "nothing to do" from "not checked".
     */
    static List<AiDailyBriefResponse.Signal> signals(
            Instant now,
            LocalDate today,
            List<Booking> arrivals,
            List<Booking> departures,
            List<Booking> pending,
            List<TourBooking> tourDepartures,
            long needsStaff,
            long unread,
            List<FeedbackRow> reviews,
            List<ReceivableDTO> receivables,
            List<Room> rooms,
            TargetProgressResponse target) {
        List<AiDailyBriefResponse.Signal> signals = new ArrayList<>();

        signals.add(signal(
                "arrivals_today",
                "Khách nhận phòng hôm nay",
                arrivals.size() + " đơn",
                arrivals.isEmpty() ? null : "Chuẩn bị phòng và giấy tờ trước giờ khách tới.",
                arrivals.isEmpty() ? QUIET : WATCH));
        signals.add(signal(
                "departures_today",
                "Khách trả phòng hôm nay",
                departures.size() + " đơn",
                departures.isEmpty() ? null : "Kiểm phòng và chốt tiền trước khi khách đi.",
                departures.isEmpty() ? QUIET : WATCH));

        Instant stale = now.minus(Duration.ofHours(STALE_PENDING_HOURS));
        long waitingTooLong = pending.stream()
                .filter(booking -> booking.getOnCreate() != null
                        && booking.getOnCreate().toInstant().isBefore(stale))
                .count();
        signals.add(signal(
                "pending_bookings",
                "Đơn phòng chờ xác nhận",
                pending.size() + " đơn",
                waitingTooLong > 0
                        ? waitingTooLong + " đơn đã chờ quá " + STALE_PENDING_HOURS + " giờ."
                        : (pending.isEmpty() ? null : "Khách vẫn đang chờ cơ sở trả lời."),
                waitingTooLong > 0 ? URGENT : (pending.isEmpty() ? QUIET : WATCH)));

        List<TourBooking> toursToday = tourDepartures.stream()
                .filter(order -> today.equals(order.getTourDate()))
                .toList();
        List<TourBooking> toursTomorrow = tourDepartures.stream()
                .filter(order -> today.plusDays(1).equals(order.getTourDate()))
                .toList();
        signals.add(signal(
                "tours_today",
                "Tour khởi hành hôm nay",
                toursToday.size() + " đơn",
                toursToday.isEmpty()
                        ? null
                        : toursToday.stream().mapToInt(TourBooking::getGuests).sum()
                                + " khách, cần người dẫn và điểm danh.",
                toursToday.isEmpty() ? QUIET : URGENT));
        signals.add(signal(
                "tours_tomorrow",
                "Tour khởi hành ngày mai",
                toursTomorrow.size() + " đơn",
                toursTomorrow.isEmpty()
                        ? null
                        : toursTomorrow.stream()
                                        .mapToInt(TourBooking::getGuests)
                                        .sum() + " khách, nên nhắc khách từ hôm nay.",
                toursTomorrow.isEmpty() ? QUIET : WATCH));

        signals.add(signal(
                "chat_waiting",
                "Tin nhắn khách chờ trả lời",
                unread + " tin",
                needsStaff > 0 ? needsStaff + " cuộc trò chuyện trợ lý AI đã chuyển cho người thật." : null,
                needsStaff > 0 ? URGENT : (unread > 0 ? WATCH : QUIET)));

        LocalDate since = today.minusDays(REVIEW_DAYS);
        long lowReviews = reviews.stream()
                .filter(review -> review.getRating() != null && review.getRating() <= LOW_RATING)
                .filter(review -> review.getReviewDate() != null
                        && !review.getReviewDate().isBefore(since))
                .count();
        signals.add(signal(
                "low_reviews",
                "Đánh giá " + LOW_RATING + " sao trở xuống trong " + REVIEW_DAYS + " ngày",
                lowReviews + " đánh giá",
                lowReviews == 0 ? null : "Trả lời sớm thì khách khác đọc thấy cơ sở có xử lý.",
                lowReviews > 0 ? URGENT : QUIET));

        BigDecimal owed = receivables.stream()
                .map(ReceivableDTO::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long oldest = receivables.stream()
                .map(ReceivableDTO::getCheckoutDate)
                .filter(date -> date != null)
                .mapToLong(date -> ChronoUnit.DAYS.between(date, today))
                .max()
                .orElse(0);
        signals.add(signal(
                "receivables",
                "Đơn đã xong chưa thu tiền",
                receivables.size() + " đơn",
                receivables.isEmpty()
                        ? null
                        : HotelKnowledge.money(owed) + ", đơn cũ nhất đã trả phòng " + oldest + " ngày.",
                oldest >= RECEIVABLE_OVERDUE_DAYS ? URGENT : (receivables.isEmpty() ? QUIET : WATCH)));

        long maintenance = rooms.stream()
                .filter(room -> room.getStatus() == RoomStatus.MAINTENANCE)
                .count();
        signals.add(signal(
                "rooms_maintenance",
                "Phòng đang bảo trì",
                maintenance + "/" + rooms.size() + " phòng",
                maintenance == 0 ? null : "Phòng bảo trì không bán được, nên chốt ngày sửa xong.",
                maintenance > 0 ? WATCH : QUIET));

        signals.add(targetSignal(target));
        return signals;
    }

    /**
     * How the month's goal is going, measured against the calendar: 40% of the goal on the 20th is behind, on the
     * 5th it is not. No goal set means nothing to report rather than a zero to worry about.
     */
    static AiDailyBriefResponse.Signal targetSignal(TargetProgressResponse target) {
        if (target == null || target.getTarget() == null || target.getTarget().signum() <= 0) {
            return signal("revenue_target", "Chỉ tiêu doanh thu tháng", "chưa đặt", null, QUIET);
        }
        double achieved = target.getAchievedRate() == null ? 0 : target.getAchievedRate();
        double elapsed = target.getDaysInMonth() <= 0 ? 0 : target.getDaysElapsed() * 100.0 / target.getDaysInMonth();
        double gap = elapsed - achieved;
        String detail = "Đã qua " + Math.round(elapsed) + "% số ngày trong tháng"
                + (target.getProjectedRate() == null
                        ? "."
                        : ", dự kiến cả tháng đạt " + Math.round(target.getProjectedRate()) + "%.");
        return signal(
                "revenue_target",
                "Chỉ tiêu doanh thu tháng",
                Math.round(achieved) + "% chỉ tiêu",
                detail,
                gap >= TARGET_GAP_POINTS ? URGENT : (gap > 0 ? WATCH : QUIET));
    }

    private JsonNode write(String model, Hotel hotel, LocalDate today, List<AiDailyBriefResponse.Signal> signals) {
        ObjectNode request = ai.newRequest(model, 1200);
        request.putArray("system").add(ai.textBlock(SYSTEM, false));
        request.putArray("tools").add(tool);
        ObjectNode choice = request.putObject("tool_choice");
        choice.put("type", "tool");
        choice.put("name", "set_daily_brief");
        ((ArrayNode) request.get("messages")).add(ai.userMessage(prompt(hotel, today, signals)));
        JsonNode response = ai.createMessage(request);
        JsonNode call = AiClient.toolUse(response, "set_daily_brief");
        if (call != null) {
            return call.path("input");
        }
        JsonNode fromText = ai.jsonObjectIn(AiClient.text(response));
        if (fromText == null) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        return fromText;
    }

    /** The signals as plain lines, with their keys and levels, so a task can point back at one. */
    static String prompt(Hotel hotel, LocalDate today, List<AiDailyBriefResponse.Signal> signals) {
        StringBuilder prompt = new StringBuilder("Cơ sở: ")
                .append(hotel.getName())
                .append("\nNgày: ")
                .append(today)
                .append("\n\nSỐ LIỆU HÔM NAY\n");
        for (AiDailyBriefResponse.Signal signal : signals) {
            prompt.append("- [")
                    .append(signal.getLevel())
                    .append("] ")
                    .append(signal.getKey())
                    .append(": ")
                    .append(signal.getLabel())
                    .append(" = ")
                    .append(signal.getValue());
            if (signal.getDetail() != null) {
                prompt.append(" (").append(signal.getDetail()).append(")");
            }
            prompt.append("\n");
        }
        return prompt.toString();
    }

    /** Model output → tasks that only point at a signal that exists and is not quiet. */
    static AiDailyBriefResponse build(
            Long hotelId,
            LocalDate today,
            List<AiDailyBriefResponse.Signal> signals,
            JsonNode brief,
            List<String> notes) {
        Set<String> live = new HashSet<>();
        signals.stream()
                .filter(signal -> !QUIET.equals(signal.getLevel()))
                .forEach(signal -> live.add(signal.getKey()));
        List<AiDailyBriefResponse.Task> tasks = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int dropped = 0;
        for (JsonNode node : brief.path("tasks")) {
            String title = text(node.path("title"));
            String how = text(node.path("how"));
            String key = text(node.path("signal_key"));
            if (title == null || how == null || tasks.size() >= 5) {
                continue;
            }
            // A task about a quiet signal - or one nobody counted - is the model making work up.
            if (key == null || !live.contains(key) || !seen.add(key)) {
                dropped++;
                continue;
            }
            tasks.add(AiDailyBriefResponse.Task.builder()
                    .title(title)
                    .priority(AiContentAudit.priority(node.path("priority")))
                    .why(text(node.path("why")))
                    .how(how)
                    .signalKey(key)
                    .build());
        }
        if (dropped > 0) {
            notes.add("Đã bỏ " + dropped + " việc không khớp số liệu nào đang cần xử lý.");
        }
        return AiDailyBriefResponse.builder()
                .hotelId(hotelId)
                .date(today)
                .headline(text(brief.path("headline")))
                .signals(signals)
                .tasks(tasks)
                .notes(notes)
                .build();
    }

    private static AiDailyBriefResponse.Signal signal(
            String key, String label, String value, String detail, String level) {
        return AiDailyBriefResponse.Signal.builder()
                .key(key)
                .label(label)
                .value(value)
                .detail(detail)
                .level(level)
                .build();
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().strip();
    }
}
