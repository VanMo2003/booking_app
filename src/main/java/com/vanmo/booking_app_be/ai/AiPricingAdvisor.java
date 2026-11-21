package com.vanmo.booking_app_be.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vanmo.booking_app_be.dto.response.AiPricingAdviceResponse;
import com.vanmo.booking_app_be.dto.response.RevenueByRoomTypeDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.WeekdayStatsDTO;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.service.ReportService;
import com.vanmo.booking_app_be.utils.TextNormalizer;

/**
 * Advice on a branch's room prices, per room type, from that branch's own report figures: how full each type was,
 * what a night really went for against its list price, which weekdays are weak, and what is already booked for the
 * weeks ahead.
 *
 * <p>Nothing here changes a price. The model is given the figures and may only answer with room types that exist
 * (anything else is dropped), a direction (RAISE / HOLD / LOWER) and a percentage the backend clamps; the suggested
 * price itself is worked out here from the list price, so the number a manager sees is arithmetic, not a guess.
 */
@Service
public class AiPricingAdvisor {

    /** Days of history the advice is based on. */
    static final int HISTORY_DAYS = 30;

    /** Days ahead the "already booked" figures cover. */
    static final int OUTLOOK_DAYS = 30;

    /** The most the assistant may suggest moving a price by. */
    static final int MAX_PERCENT = 30;

    private static final String SYSTEM =
            """
			Bạn là người tư vấn giá phòng cho một cơ sở lưu trú, dựa trên số liệu kinh doanh của chính cơ sở đó. Hãy gọi set_pricing_advice.
			Quy tắc:
			- CHỈ dùng số trong mục "SỐ LIỆU" và chỉ nói về những loại phòng có trong danh sách. Không bịa loại phòng, không bịa số, không so sánh với khách sạn khác (bạn không có dữ liệu đó).
			- Mỗi loại phòng một mục: action là RAISE (tăng giá), HOLD (giữ) hoặc LOWER (giảm). RAISE/LOWER thì cho percent từ 3 đến 20; HOLD thì bỏ trống percent.
			- Cách đọc số liệu: lấp phòng cao (trên khoảng 75%) và ADR gần hoặc trên giá niêm yết thì còn chỗ để tăng; lấp phòng thấp (dưới khoảng 40%) và sắp tới cũng ít khách thì nên giảm hoặc làm khuyến mãi; ADR thấp hơn giá niêm yết nhiều nghĩa là đang bán dưới giá, giảm giá nữa ít tác dụng.
			- Loại phòng có rất ít đêm bán được trong kỳ thì nói thẳng là chưa đủ dữ liệu và để HOLD.
			- reason: một câu tiếng Việt, nêu đúng con số dẫn tới kết luận (lấp phòng, ADR, giá niêm yết, lượng đã đặt trước).
			- summary: 2-3 câu về tình hình chung. risks: 1-3 điều cần cân nhắc khi làm theo.
			- Văn bản thường, không markdown, không emoji. Tiền ghi dạng 850.000 ₫, tỉ lệ ghi dạng 42,5%.
			""";

    private static final String TOOL =
            """
			{
			"name": "set_pricing_advice",
			"description": "Gợi ý điều chỉnh giá theo từng loại phòng.",
			"input_schema": {
				"type": "object",
				"properties": {
				"summary": {"type": "string"},
				"items": {
					"type": "array",
					"items": {
					"type": "object",
					"properties": {
						"room_type": {"type": "string", "description": "Tên loại phòng, đúng như trong danh sách"},
						"action": {"type": "string", "enum": ["RAISE", "HOLD", "LOWER"]},
						"percent": {"type": "integer", "description": "3-20, chỉ khi tăng hoặc giảm"},
						"reason": {"type": "string"}
					},
					"required": ["room_type", "action", "reason"]
					}
				},
				"risks": {"type": "array", "items": {"type": "string"}}
				},
				"required": ["summary", "items"]
			}
			}
			""";

    private final AiClient ai;
    private final AiRateLimiter rateLimiter;
    private final ReportService reportService;
    private final JsonNode tool;

    public AiPricingAdvisor(AiClient ai, AiRateLimiter rateLimiter, ReportService reportService)
            throws JsonProcessingException {
        this.ai = ai;
        this.rateLimiter = rateLimiter;
        this.reportService = reportService;
        this.tool = ai.json().readTree(TOOL);
    }

    public AiPricingAdviceResponse advise(Long hotelId) {
        if (!ai.isEnabled()) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        rateLimiter.check("pricing:" + hotelId, 6, Duration.ofMinutes(10));
        LocalDate today = LocalDate.now(ChatAssistant.ZONE);
        LocalDate from = today.minusDays(HISTORY_DAYS - 1L);

        // The ordinary report service: its own access checks, and the numbers the report screens show.
        RevenueSummaryResponse summary = reportService.getRevenueSummary(hotelId, from, today);
        List<RevenueByRoomTypeDTO> types = reportService.getRevenueByRoomType(hotelId, from, today);
        List<WeekdayStatsDTO> weekdays = reportService.getWeekdayPattern(hotelId, from, today);
        RevenueOutlookResponse outlook = reportService.getRevenueOutlook(hotelId, OUTLOOK_DAYS);

        List<String> notes = new ArrayList<>();
        if (types.isEmpty()) {
            notes.add("Cơ sở chưa có loại phòng nào để tư vấn giá.");
            return AiPricingAdviceResponse.builder()
                    .hotelId(hotelId)
                    .from(from)
                    .to(today)
                    .outlookDays(OUTLOOK_DAYS)
                    .items(List.of())
                    .notes(notes)
                    .build();
        }

        String brief = brief(from, today, summary, types, weekdays, outlook);
        JsonNode advice = ai.withModelFallback(ai.getAssistantModels(), model -> advise(model, brief));
        return build(hotelId, from, today, types, advice, notes);
    }

    private JsonNode advise(String model, String brief) {
        ObjectNode request = ai.newRequest(model, 1500);
        request.putArray("system").add(ai.textBlock(SYSTEM, false));
        request.putArray("tools").add(tool);
        ObjectNode choice = request.putObject("tool_choice");
        choice.put("type", "tool");
        choice.put("name", "set_pricing_advice");
        ((ArrayNode) request.get("messages")).add(ai.userMessage(brief));
        JsonNode response = ai.createMessage(request);
        JsonNode call = AiClient.toolUse(response, "set_pricing_advice");
        if (call != null) {
            return call.path("input");
        }
        JsonNode fromText = ai.jsonObjectIn(AiClient.text(response));
        if (fromText == null) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        return fromText;
    }

    /** The figures as plain Vietnamese lines - the only numbers the model may use. */
    static String brief(
            LocalDate from,
            LocalDate to,
            RevenueSummaryResponse summary,
            List<RevenueByRoomTypeDTO> types,
            List<WeekdayStatsDTO> weekdays,
            RevenueOutlookResponse outlook) {
        StringBuilder brief = new StringBuilder("SỐ LIỆU\n");
        brief.append("Kỳ xem: ").append(from).append(" đến ").append(to).append("\n");
        RevenuePeriodDTO now = summary == null ? null : summary.getCurrent();
        if (now != null) {
            brief.append("Toàn cơ sở: lấp phòng ")
                    .append(AiReportAnalyst.number(now.getOccupancyRate()))
                    .append("%, ADR ")
                    .append(AiReportAnalyst.money(now.getAdr()))
                    .append(", RevPAR ")
                    .append(AiReportAnalyst.money(now.getRevpar()))
                    .append(", ")
                    .append(now.getRoomNightsSold())
                    .append("/")
                    .append(now.getAvailableRoomNights())
                    .append(" đêm phòng bán được, huỷ ")
                    .append(AiReportAnalyst.number(now.getCancellationRate()))
                    .append("%\n");
        }
        brief.append("\nTheo loại phòng:\n");
        for (RevenueByRoomTypeDTO type : types) {
            brief.append("- ")
                    .append(type.getRoomTypeName())
                    .append(": ")
                    .append(type.getRooms())
                    .append(" phòng, giá niêm yết ")
                    .append(AiReportAnalyst.money(type.getListPrice()))
                    .append(", ADR thực ")
                    .append(AiReportAnalyst.money(type.getAdr()))
                    .append(", lấp phòng ")
                    .append(AiReportAnalyst.number(type.getOccupancyRate()))
                    .append("%, bán được ")
                    .append(type.getRoomNights())
                    .append(" đêm, doanh thu ")
                    .append(AiReportAnalyst.money(type.getTotalRevenue()))
                    .append("\n");
        }
        if (weekdays != null && !weekdays.isEmpty()) {
            brief.append("\nTheo ngày trong tuần (lấp phòng trung bình):\n");
            for (WeekdayStatsDTO day : weekdays) {
                brief.append("- ")
                        .append(weekdayName(day.getDayOfWeek()))
                        .append(": ")
                        .append(AiReportAnalyst.number(day.getAverageOccupancy()))
                        .append("%, doanh thu trung bình ")
                        .append(AiReportAnalyst.money(day.getAverageRevenue()))
                        .append("\n");
            }
        }
        if (outlook != null) {
            brief.append("\nĐã đặt trước cho ")
                    .append(OUTLOOK_DAYS)
                    .append(" ngày tới: ")
                    .append(outlook.getBookedRoomNights())
                    .append("/")
                    .append(outlook.getAvailableRoomNights())
                    .append(" đêm phòng, lấp phòng theo sổ ")
                    .append(AiReportAnalyst.number(outlook.getOccupancyOnTheBooks()))
                    .append("%, doanh thu dự kiến ")
                    .append(AiReportAnalyst.money(outlook.getTotalExpected()))
                    .append("\n");
        }
        return brief.toString();
    }

    /**
     * Model output → advice the manager can act on: only room types that exist, a clamped percentage, and the
     * suggested price worked out here from the list price.
     */
    static AiPricingAdviceResponse build(
            Long hotelId,
            LocalDate from,
            LocalDate to,
            List<RevenueByRoomTypeDTO> types,
            JsonNode advice,
            List<String> notes) {
        List<AiPricingAdviceResponse.Item> items = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int invented = 0;
        for (JsonNode node : advice.path("items")) {
            String name = text(node.path("room_type"));
            RevenueByRoomTypeDTO type = match(types, name);
            if (type == null || !seen.add(type.getRoomTypeId() == null ? -1L : type.getRoomTypeId())) {
                invented++;
                continue;
            }
            String action = action(node.path("action"));
            Integer percent = percent(node.path("percent"), action);
            items.add(AiPricingAdviceResponse.Item.builder()
                    .roomTypeId(type.getRoomTypeId())
                    .roomTypeName(type.getRoomTypeName())
                    .action(action)
                    .percent(percent)
                    .reason(text(node.path("reason")))
                    .listPrice(type.getListPrice())
                    .adr(type.getAdr())
                    .occupancyRate(type.getOccupancyRate())
                    .rooms(type.getRooms())
                    .suggestedPrice(suggestedPrice(type.getListPrice(), action, percent))
                    .build());
        }
        if (invented > 0) {
            notes.add("Đã bỏ " + invented + " gợi ý không khớp loại phòng nào của cơ sở.");
        }
        // Every type the advice skipped is still listed, as "giữ nguyên", so the manager sees the whole picture.
        for (RevenueByRoomTypeDTO type : types) {
            boolean covered =
                    items.stream().anyMatch(item -> item.getRoomTypeName().equals(type.getRoomTypeName()));
            if (!covered) {
                items.add(AiPricingAdviceResponse.Item.builder()
                        .roomTypeId(type.getRoomTypeId())
                        .roomTypeName(type.getRoomTypeName())
                        .action("HOLD")
                        .listPrice(type.getListPrice())
                        .adr(type.getAdr())
                        .occupancyRate(type.getOccupancyRate())
                        .rooms(type.getRooms())
                        .build());
            }
        }
        return AiPricingAdviceResponse.builder()
                .hotelId(hotelId)
                .from(from)
                .to(to)
                .outlookDays(OUTLOOK_DAYS)
                .summary(text(advice.path("summary")))
                .items(items)
                .risks(lines(advice.path("risks"), 3))
                .notes(notes)
                .build();
    }

    /** The list price moved by the suggested percentage, rounded to the nearest thousand đồng. */
    static BigDecimal suggestedPrice(BigDecimal listPrice, String action, Integer percent) {
        if (listPrice == null || percent == null || listPrice.signum() <= 0 || "HOLD".equals(action)) {
            return null;
        }
        int signed = "LOWER".equals(action) ? -percent : percent;
        BigDecimal moved = listPrice
                .multiply(BigDecimal.valueOf(100 + signed))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return moved.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(1000));
    }

    /** The room type the model named, matched accent- and case-insensitively; null when no type fits. */
    static RevenueByRoomTypeDTO match(List<RevenueByRoomTypeDTO> types, String name) {
        if (name == null) {
            return null;
        }
        for (RevenueByRoomTypeDTO type : types) {
            if (TextNormalizer.looselyMatches(type.getRoomTypeName(), name)) {
                return type;
            }
        }
        return null;
    }

    static String action(JsonNode node) {
        String raw = text(node);
        if (raw == null) {
            return "HOLD";
        }
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "RAISE", "LOWER" -> raw.toUpperCase(Locale.ROOT);
            default -> "HOLD";
        };
    }

    static Integer percent(JsonNode node, String action) {
        if ("HOLD".equals(action) || node == null || !node.canConvertToInt()) {
            return null;
        }
        int value = Math.abs(node.asInt());
        if (value == 0) {
            return null;
        }
        return Math.min(value, MAX_PERCENT);
    }

    private static String weekdayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Thứ Hai";
            case 2 -> "Thứ Ba";
            case 3 -> "Thứ Tư";
            case 4 -> "Thứ Năm";
            case 5 -> "Thứ Sáu";
            case 6 -> "Thứ Bảy";
            case 7 -> "Chủ Nhật";
            default -> "Ngày " + dayOfWeek;
        };
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().strip();
    }

    private static List<String> lines(JsonNode node, int max) {
        List<String> lines = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String line = item.asText("").strip().replace("\n", " ");
                if (!line.isEmpty() && lines.size() < max) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }
}
