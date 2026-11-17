package com.vanmo.booking_app_be.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vanmo.booking_app_be.dto.response.AiPromotionAdviceResponse;
import com.vanmo.booking_app_be.dto.response.BranchPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.WeekdayStatsDTO;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.Voucher;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.repository.HotelRepository;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.repository.VoucherRepository;
import com.vanmo.booking_app_be.service.ReportService;

/**
 * Which discount campaigns are worth running, read out of the chain's own figures: the weekdays that stay empty,
 * the branches falling behind, the tours nobody books, and how much of the next month is already on the books.
 *
 * <p>Nothing is created. The owner reads a campaign and then writes the voucher on the voucher screen, which is
 * the only thing that makes a real discount - so a model mistake costs nobody money. The model also never
 * supplies a figure that matters: {@link #build} clamps the percentage to {@link #MAX_PERCENT}, works the
 * suggested minimum order out from the period's own ADR, dates the campaign from the horizon bucket the model
 * picked, drops a branch that is not in this chain and a tour that does not exist.
 */
@Service
public class AiPromotionAdvisor {

    /** How far back the figures are read. */
    static final int HISTORY_DAYS = 30;

    /** How far ahead "already on the books" is measured. */
    static final int OUTLOOK_DAYS = 30;

    /** A campaign deeper than this is a decision for a person, not a suggestion. */
    static final int MAX_PERCENT = 25;

    /** At most this many campaigns - a list nobody reads is the same as no advice. */
    static final int MAX_CAMPAIGNS = 4;

    private static final String SYSTEM =
            """
			Bạn giúp chủ một chuỗi lưu trú chọn chương trình giảm giá nên chạy. Hãy gọi set_promotion_advice.
			Quy tắc:
			- Chỉ dựa vào "SỐ LIỆU" bên dưới. Không bịa con số, không nhắc cơ sở hay tour không có trong đó.
			- campaigns: tối đa 4 chương trình, cái đáng chạy nhất trước. Mỗi chương trình nhằm vào một chỗ trống thấy được trong số liệu (ngày trong tuần lấp phòng thấp, cơ sở đang hụt, tour ít khách, tháng tới còn trống nhiều).
			- scope là CHAIN khi cả chuỗi đều cần, là BRANCH khi chỉ một cơ sở cần - khi đó hotel_name phải đúng tên một cơ sở trong số liệu.
			- discount_percent là số nguyên 5 đến 25. Giảm sâu hơn là quyết định của chủ chuỗi, không phải của bạn.
			- horizon_days chỉ được là 7, 14 hoặc 30 - chương trình nên chạy trong bao nhiêu ngày tới.
			- why: một câu, nêu đúng con số trong số liệu làm lý do.
			- tours: tên tour trong số liệu nên đẩy kèm chương trình; không có thì để mảng rỗng.
			- Đã có mã giảm giá đang chạy cho đúng chỗ đó thì đừng đề xuất trùng, nói trong summary là đã có.
			- Không đặt mã giảm giá, không nói về giá niêm yết từng phòng, không hứa gì với khách.
			- summary: 2-3 câu tiếng Việt, giọng đồng nghiệp. Văn bản thường, không markdown, không emoji.
			""";

    private static final String TOOL =
            """
			{
			"name": "set_promotion_advice",
			"description": "Chương trình giảm giá nên chạy.",
			"input_schema": {
				"type": "object",
				"properties": {
				"summary": {"type": "string"},
				"campaigns": {
					"type": "array",
					"items": {
					"type": "object",
					"properties": {
						"title": {"type": "string"},
						"scope": {"type": "string", "enum": ["CHAIN", "BRANCH"]},
						"hotel_name": {"type": "string"},
						"discount_percent": {"type": "integer"},
						"horizon_days": {"type": "integer", "enum": [7, 14, 30]},
						"why": {"type": "string"},
						"tours": {"type": "array", "items": {"type": "string"}}
					},
					"required": ["title", "scope", "discount_percent", "horizon_days", "why"]
					}
				}
				},
				"required": ["summary", "campaigns"]
			}
			}
			""";

    private static final String[] WEEKDAYS = {
        "", "Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"
    };

    private final AiClient ai;
    private final AiRateLimiter rateLimiter;
    private final ReportService reportService;
    private final HotelRepository hotelRepository;
    private final TourRepository tourRepository;
    private final VoucherRepository voucherRepository;
    private final JsonNode tool;

    public AiPromotionAdvisor(
            AiClient ai,
            AiRateLimiter rateLimiter,
            ReportService reportService,
            HotelRepository hotelRepository,
            TourRepository tourRepository,
            VoucherRepository voucherRepository)
            throws JsonProcessingException {
        this.ai = ai;
        this.rateLimiter = rateLimiter;
        this.reportService = reportService;
        this.hotelRepository = hotelRepository;
        this.tourRepository = tourRepository;
        this.voucherRepository = voucherRepository;
        this.tool = ai.json().readTree(TOOL);
    }

    @Transactional(readOnly = true)
    public AiPromotionAdviceResponse of(Long hotelChainId) {
        LocalDate today = LocalDate.now(ChatAssistant.ZONE);
        LocalDate from = today.minusDays(HISTORY_DAYS - 1L);

        // Every figure comes from the ordinary report service, so the chain-owner guard applies here too and the
        // numbers are the ones the report screens show.
        RevenueSummaryResponse summary = reportService.getRevenueSummaryForChain(hotelChainId, from, today);
        List<BranchPerformanceDTO> branches = reportService.getBranchPerformanceForChain(hotelChainId, from, today);
        List<WeekdayStatsDTO> weekdays = reportService.getWeekdayPatternForChain(hotelChainId, from, today);
        List<RevenueByTourDTO> tours = reportService.getRevenueByTourForChain(hotelChainId, from, today);
        RevenueOutlookResponse outlook = reportService.getRevenueOutlookForChain(hotelChainId, OUTLOOK_DAYS);

        List<Hotel> chainHotels = hotelRepository.findAllByHotelChain_Id(hotelChainId);
        List<Voucher> running = voucherRepository.findAllByHotelChain_IdOrderByIdDesc(hotelChainId).stream()
                .filter(voucher -> isRunning(voucher, today))
                .toList();
        List<String> runningLabels =
                running.stream().map(AiPromotionAdvisor::label).toList();

        if (!ai.isEnabled()) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        rateLimiter.check("promotion:" + hotelChainId, 6, Duration.ofMinutes(10));

        String brief = brief(from, today, summary, branches, weekdays, tours, outlook, runningLabels);
        JsonNode advice = ai.withModelFallback(ai.getAssistantModels(), model -> advise(model, brief));
        return build(
                hotelChainId,
                from,
                today,
                summary,
                outlook,
                chainHotels,
                tourNames(chainHotels),
                runningLabels,
                advice,
                new ArrayList<>());
    }

    /** A voucher counts as running when a guest could use it today. */
    static boolean isRunning(Voucher voucher, LocalDate today) {
        return voucher.isActive()
                && voucher.getStartsAt() != null
                && voucher.getEndsAt() != null
                && !voucher.getStartsAt().isAfter(today)
                && !voucher.getEndsAt().isBefore(today);
    }

    /** "GIAMHE10 - Giảm hè (toàn chuỗi, đến 2026-12-31)". */
    static String label(Voucher voucher) {
        String where =
                voucher.getHotel() == null ? "toàn chuỗi" : voucher.getHotel().getName();
        return voucher.getCode() + " - " + voucher.getTitle() + " (" + where + ", đến " + voucher.getEndsAt() + ")";
    }

    private Set<String> tourNames(List<Hotel> chainHotels) {
        Set<String> names = new HashSet<>();
        for (Hotel hotel : chainHotels) {
            tourRepository.findAllByHotel_Id(hotel.getId()).stream()
                    .map(Tour::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .forEach(name -> names.add(name.strip()));
        }
        return names;
    }

    /** The chain's figures as plain text: where the holes are, branch by branch, weekday by weekday. */
    static String brief(
            LocalDate from,
            LocalDate to,
            RevenueSummaryResponse summary,
            List<BranchPerformanceDTO> branches,
            List<WeekdayStatsDTO> weekdays,
            List<RevenueByTourDTO> tours,
            RevenueOutlookResponse outlook,
            List<String> runningVouchers) {
        StringBuilder brief = new StringBuilder("SỐ LIỆU\n");
        brief.append("Kỳ xem: ").append(from).append(" đến ").append(to).append("\n");
        RevenuePeriodDTO now = summary == null ? null : summary.getCurrent();
        if (now != null) {
            brief.append("Toàn chuỗi: lấp phòng ")
                    .append(AiReportAnalyst.number(now.getOccupancyRate()))
                    .append("%, ADR ")
                    .append(AiReportAnalyst.money(now.getAdr()))
                    .append(", doanh thu ")
                    .append(AiReportAnalyst.money(now.getTotalRevenue()))
                    .append(", ")
                    .append(now.getCompletedBookings())
                    .append(" đơn phòng và ")
                    .append(now.getCompletedTours())
                    .append(" đơn tour hoàn tất\n");
        }

        brief.append("\nTheo cơ sở:\n");
        if (branches.isEmpty()) {
            brief.append("- (chưa có cơ sở nào có số liệu)\n");
        }
        for (BranchPerformanceDTO branch : branches) {
            RevenuePeriodDTO period = branch.getCurrent();
            brief.append("- ")
                    .append(branch.getHotelName())
                    .append(": doanh thu ")
                    .append(AiReportAnalyst.money(period == null ? null : period.getTotalRevenue()))
                    .append(" (")
                    .append(AiReportAnalyst.number(branch.getRevenueShare()))
                    .append("% của chuỗi), lấp phòng ")
                    .append(AiReportAnalyst.number(period == null ? null : period.getOccupancyRate()))
                    .append("%");
            if (branch.getRevenueGrowth() != null) {
                brief.append(", so kỳ trước ")
                        .append(branch.getRevenueGrowth() >= 0 ? "+" : "")
                        .append(AiReportAnalyst.number(branch.getRevenueGrowth()))
                        .append("%");
            }
            brief.append("\n");
        }

        brief.append("\nLấp phòng theo ngày trong tuần:\n");
        for (WeekdayStatsDTO weekday : weekdays) {
            brief.append("- ")
                    .append(weekdayName(weekday.getDayOfWeek()))
                    .append(": lấp phòng ")
                    .append(AiReportAnalyst.number(weekday.getAverageOccupancy()))
                    .append("%, doanh thu trung bình ")
                    .append(AiReportAnalyst.money(weekday.getAverageRevenue()))
                    .append("\n");
        }

        brief.append("\nTour:\n");
        if (tours.isEmpty()) {
            brief.append("- (chưa có đơn tour nào trong kỳ)\n");
        }
        for (RevenueByTourDTO tour : tours) {
            brief.append("- ")
                    .append(tour.getTourName())
                    .append(": ")
                    .append(tour.getOrders())
                    .append(" đơn, ")
                    .append(tour.getGuests())
                    .append(" khách, doanh thu ")
                    .append(AiReportAnalyst.money(tour.getTourRevenue()));
            if (tour.getSeatsOffered() > 0) {
                brief.append(", lấp chỗ ")
                        .append(AiReportAnalyst.number(tour.getFillRate()))
                        .append("%");
            }
            brief.append("\n");
        }

        if (outlook != null) {
            brief.append("\n")
                    .append(OUTLOOK_DAYS)
                    .append(" ngày tới đã đặt trước: lấp phòng ")
                    .append(AiReportAnalyst.number(outlook.getOccupancyOnTheBooks()))
                    .append("%, dự kiến thu ")
                    .append(AiReportAnalyst.money(outlook.getTotalExpected()))
                    .append(" từ ")
                    .append(outlook.getBookings())
                    .append(" đơn phòng và ")
                    .append(outlook.getTours())
                    .append(" đơn tour\n");
        }

        brief.append("\nMã giảm giá đang chạy:\n");
        if (runningVouchers.isEmpty()) {
            brief.append("- (không có mã nào đang chạy)\n");
        }
        runningVouchers.forEach(voucher -> brief.append("- ").append(voucher).append("\n"));
        return brief.toString();
    }

    static String weekdayName(int dayOfWeek) {
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? WEEKDAYS[dayOfWeek] : "(không rõ)";
    }

    private JsonNode advise(String model, String brief) {
        ObjectNode request = ai.newRequest(model, 1400);
        request.putArray("system").add(ai.textBlock(SYSTEM, false));
        request.putArray("tools").add(tool);
        ObjectNode choice = request.putObject("tool_choice");
        choice.put("type", "tool");
        choice.put("name", "set_promotion_advice");
        ((ArrayNode) request.get("messages")).add(ai.userMessage(brief));
        JsonNode response = ai.createMessage(request);
        JsonNode call = AiClient.toolUse(response, "set_promotion_advice");
        if (call != null) {
            return call.path("input");
        }
        JsonNode fromText = ai.jsonObjectIn(AiClient.text(response));
        if (fromText == null) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        return fromText;
    }

    /** Model output → campaigns whose branch, percentage, dates and minimum order are all the backend's. */
    static AiPromotionAdviceResponse build(
            Long hotelChainId,
            LocalDate from,
            LocalDate to,
            RevenueSummaryResponse summary,
            RevenueOutlookResponse outlook,
            List<Hotel> chainHotels,
            Set<String> tourNames,
            List<String> runningVouchers,
            JsonNode advice,
            List<String> notes) {
        Map<String, Hotel> byName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        chainHotels.forEach(hotel -> byName.put(hotel.getName().strip(), hotel));
        Map<String, String> toursByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tourNames.forEach(name -> toursByName.put(name, name));

        RevenuePeriodDTO now = summary == null ? null : summary.getCurrent();
        BigDecimal minOrder = minOrder(now);
        List<AiPromotionAdviceResponse.Campaign> campaigns = new ArrayList<>();
        int dropped = 0;
        for (JsonNode node : advice.path("campaigns")) {
            if (campaigns.size() >= MAX_CAMPAIGNS) {
                break;
            }
            String title = text(node.path("title"));
            String why = text(node.path("why"));
            if (title == null || why == null) {
                dropped++;
                continue;
            }
            boolean branchScope = "BRANCH".equalsIgnoreCase(text(node.path("scope")));
            Hotel hotel = branchScope ? byName.get(name(node.path("hotel_name"))) : null;
            // A branch campaign for a branch outside this chain would send the owner to a screen they cannot use.
            if (branchScope && hotel == null) {
                dropped++;
                continue;
            }
            int days = horizon(node.path("horizon_days"));
            List<String> pushedTours = new ArrayList<>();
            for (JsonNode tourNode : node.path("tours")) {
                String real = toursByName.get(name(tourNode));
                if (real != null && !pushedTours.contains(real)) {
                    pushedTours.add(real);
                }
            }
            campaigns.add(AiPromotionAdviceResponse.Campaign.builder()
                    .title(title)
                    .scope(branchScope ? "BRANCH" : "CHAIN")
                    .hotelId(hotel == null ? null : hotel.getId())
                    .hotelName(hotel == null ? null : hotel.getName())
                    .discountPercent(percent(node.path("discount_percent")))
                    .minOrderAmount(minOrder)
                    .startsAt(to)
                    .endsAt(to.plusDays(days - 1L))
                    .why(why)
                    .tours(pushedTours)
                    .build());
        }
        if (dropped > 0) {
            notes.add("Đã bỏ " + dropped + " chương trình không khớp cơ sở nào của chuỗi.");
        }
        if (campaigns.isEmpty()) {
            notes.add("Số liệu kỳ này chưa cho thấy chỗ nào cần giảm giá.");
        }
        return AiPromotionAdviceResponse.builder()
                .hotelChainId(hotelChainId)
                .from(from)
                .to(to)
                .occupancyRate(now == null ? null : now.getOccupancyRate())
                .adr(now == null ? null : now.getAdr())
                .outlookOccupancy(outlook == null ? null : outlook.getOccupancyOnTheBooks())
                .runningVouchers(runningVouchers)
                .campaigns(campaigns)
                .notes(notes)
                .build();
    }

    /** 5-{@value #MAX_PERCENT}%; anything else is pulled back to the nearest end of that range. */
    static int percent(JsonNode node) {
        int raw = node == null ? 0 : node.asInt(0);
        return Math.min(MAX_PERCENT, Math.max(5, raw));
    }

    /** Only 7, 14 or 30 days - anything else becomes 14, so the dates are never the model's invention. */
    static int horizon(JsonNode node) {
        return switch (node == null ? 0 : node.asInt(0)) {
            case 7 -> 7;
            case 30 -> 30;
            default -> 14;
        };
    }

    /**
     * A minimum order of about one night at the period's own ADR: enough that the discount comes off a real
     * booking, not a token one. Rounded to the nearest ten thousand đồng; no ADR means no minimum.
     */
    static BigDecimal minOrder(RevenuePeriodDTO period) {
        BigDecimal adr = period == null ? null : period.getAdr();
        if (adr == null || adr.signum() <= 0) {
            return null;
        }
        return adr.divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(10_000));
    }

    private static String name(JsonNode node) {
        String value = text(node);
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().strip();
    }
}
