package com.vanmo.booking_app_be.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vanmo.booking_app_be.dto.response.AiContentAuditResponse;
import com.vanmo.booking_app_be.entity.Dish;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.repository.AmenityRepository;
import com.vanmo.booking_app_be.repository.DishRepository;
import com.vanmo.booking_app_be.repository.HotelImageRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.ServiceRepository;
import com.vanmo.booking_app_be.repository.TourRepository;
import com.vanmo.booking_app_be.security.AccessGuard;

/**
 * Whether a branch's page is ready to sell: photos on its rooms, a description worth reading, tours with an
 * itinerary, a menu with prices. The checks and the score are counted here from the branch's own rows - the model
 * is told the results and only says, in the manager's words, what to fix first and why it costs them bookings.
 *
 * <p>So nothing in the numbers is invented, and the advice can still be read as advice: a missing photo is a fact,
 * "ảnh phòng là thứ khách xem đầu tiên" is wording.
 */
@Service
public class AiContentAudit {

    /** A description shorter than this reads as a placeholder. */
    private static final int MIN_DESCRIPTION_CHARS = 120;

    private static final String SYSTEM =
            """
			Bạn giúp quản lý một cơ sở lưu trú biết nên hoàn thiện trang bán phòng ở đâu trước. Hãy gọi set_content_audit.
			Quy tắc:
			- Mục "KẾT QUẢ KIỂM TRA" là sự thật hệ thống đã đếm. Không bịa thêm con số, không nói ngược lại nó, không nhắc tiêu chí không có trong danh sách.
			- items: chỉ về những tiêu chí CHƯA đạt, tối đa 5 mục, quan trọng nhất trước. Mỗi mục: title ngắn, priority là HIGH (ảnh hưởng trực tiếp tới việc khách quyết định đặt), MEDIUM, LOW; why một câu nói mất gì; how một câu việc cần làm; check_key đúng key của tiêu chí đó.
			- Mọi tiêu chí đều đạt thì items để mảng rỗng và summary nói rõ trang đã đủ.
			- summary: 2-3 câu tiếng Việt, giọng đồng nghiệp, không khen sáo.
			- Văn bản thường, không markdown, không emoji, không nhắc điểm số.
			""";

    private static final String TOOL =
            """
			{
			"name": "set_content_audit",
			"description": "Việc nên làm để hoàn thiện trang của cơ sở.",
			"input_schema": {
				"type": "object",
				"properties": {
				"summary": {"type": "string"},
				"items": {
					"type": "array",
					"items": {
					"type": "object",
					"properties": {
						"title": {"type": "string"},
						"priority": {"type": "string", "enum": ["HIGH", "MEDIUM", "LOW"]},
						"why": {"type": "string"},
						"how": {"type": "string"},
						"check_key": {"type": "string"}
					},
					"required": ["title", "priority", "how"]
					}
				}
				},
				"required": ["summary", "items"]
			}
			}
			""";

    private final AiClient ai;
    private final AiRateLimiter rateLimiter;
    private final AccessGuard accessGuard;
    private final HotelKnowledge knowledge;
    private final RoomRepository roomRepository;
    private final TourRepository tourRepository;
    private final DishRepository dishRepository;
    private final ServiceRepository serviceRepository;
    private final AmenityRepository amenityRepository;
    private final HotelImageRepository hotelImageRepository;
    private final JsonNode tool;

    public AiContentAudit(
            AiClient ai,
            AiRateLimiter rateLimiter,
            AccessGuard accessGuard,
            HotelKnowledge knowledge,
            RoomRepository roomRepository,
            TourRepository tourRepository,
            DishRepository dishRepository,
            ServiceRepository serviceRepository,
            AmenityRepository amenityRepository,
            HotelImageRepository hotelImageRepository)
            throws JsonProcessingException {
        this.ai = ai;
        this.rateLimiter = rateLimiter;
        this.accessGuard = accessGuard;
        this.knowledge = knowledge;
        this.roomRepository = roomRepository;
        this.tourRepository = tourRepository;
        this.dishRepository = dishRepository;
        this.serviceRepository = serviceRepository;
        this.amenityRepository = amenityRepository;
        this.hotelImageRepository = hotelImageRepository;
        this.tool = ai.json().readTree(TOOL);
    }

    @Transactional(readOnly = true)
    public AiContentAuditResponse of(Long hotelId) {
        accessGuard.requireBranchManagerOrAbove(hotelId);
        Hotel hotel = knowledge.hotel(hotelId);
        List<AiContentAuditResponse.Check> checks = checks(
                hotel,
                roomRepository.findAllByHotel_Id(hotelId),
                tourRepository.findAllByHotel_Id(hotelId),
                dishRepository.findAllByHotel_Id(hotelId),
                serviceRepository.findAllByHotel_Id(hotelId).size(),
                amenityRepository.findAllByHotel_Id(hotelId).size(),
                hotelImageRepository.findAllByHotel_Id(hotelId).size());
        int score = score(checks);
        List<String> notes = new ArrayList<>();

        if (!ai.isEnabled()) {
            // The checks are the useful half and need no model: hand them over without advice.
            notes.add("Máy chủ chưa bật AI nên chưa có phần gợi ý, nhưng kết quả kiểm tra vẫn đúng.");
            return AiContentAuditResponse.builder()
                    .hotelId(hotelId)
                    .score(score)
                    .checks(checks)
                    .items(List.of())
                    .notes(notes)
                    .build();
        }
        rateLimiter.check("content-audit:" + hotelId, 6, Duration.ofMinutes(10));
        JsonNode advice = ai.withModelFallback(ai.getAssistantModels(), model -> advise(model, hotel, checks));
        return build(hotelId, score, checks, advice, notes);
    }

    /**
     * Every check, counted from the branch's rows. A check with no rows to judge (no tour at all) passes: the
     * branch is not obliged to sell tours, and nagging about an empty section is noise.
     */
    static List<AiContentAuditResponse.Check> checks(
            Hotel hotel,
            List<Room> rooms,
            List<Tour> tours,
            List<Dish> dishes,
            int services,
            int amenities,
            int hotelImages) {
        List<AiContentAuditResponse.Check> checks = new ArrayList<>();

        String description =
                hotel.getDescription() == null ? "" : hotel.getDescription().strip();
        checks.add(check(
                "hotel_description",
                "Cơ sở có mô tả đủ dài",
                description.length() >= MIN_DESCRIPTION_CHARS,
                null,
                null,
                "Khách đọc mô tả để biết cơ sở phù hợp với mình không."));
        checks.add(check(
                "hotel_photos",
                "Cơ sở có ít nhất 3 ảnh",
                hotelImages >= 3 || hotel.getPathImage() != null,
                hotelImages,
                3,
                "Ảnh là thứ khách xem trước khi đọc bất cứ chữ nào."));
        checks.add(check(
                "hotel_amenities",
                "Có khai tiện ích chung",
                amenities > 0,
                amenities,
                null,
                "Tiện ích là điều kiện khách lọc khi tìm phòng."));
        checks.add(check(
                "hotel_services",
                "Có khai dịch vụ tính thêm",
                services > 0,
                services,
                null,
                "Dịch vụ giúp tăng doanh thu mỗi đơn."));

        long roomsWithPhoto =
                rooms.stream().filter(room -> notBlank(room.getPathImage())).count();
        long roomsPriced = rooms.stream()
                .filter(room -> room.getPrice() != null && room.getPrice().signum() > 0)
                .count();
        checks.add(check(
                "rooms_exist",
                "Có ít nhất một phòng",
                !rooms.isEmpty(),
                rooms.size(),
                1,
                "Chưa có phòng thì chưa bán được gì."));
        checks.add(check(
                "rooms_with_photo",
                "Mọi phòng đều có ảnh",
                rooms.isEmpty() || roomsWithPhoto == rooms.size(),
                (int) roomsWithPhoto,
                rooms.size(),
                "Phòng không ảnh gần như không được chọn."));
        checks.add(check(
                "rooms_priced",
                "Mọi phòng đều có giá",
                rooms.isEmpty() || roomsPriced == rooms.size(),
                (int) roomsPriced,
                rooms.size(),
                "Phòng giá 0 không hiện đúng khi khách tìm."));

        long toursWithItinerary = tours.stream()
                .filter(tour ->
                        tour.getItinerary() != null && !tour.getItinerary().isEmpty())
                .count();
        long toursWithDestination =
                tours.stream().filter(tour -> notBlank(tour.getDestination())).count();
        long toursWithPhoto =
                tours.stream().filter(tour -> notBlank(tour.getPathImage())).count();
        checks.add(check(
                "tours_with_destination",
                "Mọi tour đều có điểm đến",
                tours.isEmpty() || toursWithDestination == tours.size(),
                (int) toursWithDestination,
                tours.size(),
                "Không có điểm đến thì khách tìm tour không ra."));
        checks.add(check(
                "tours_with_itinerary",
                "Mọi tour đều có lịch trình",
                tours.isEmpty() || toursWithItinerary == tours.size(),
                (int) toursWithItinerary,
                tours.size(),
                "Lịch trình là phần khách đọc kỹ nhất trước khi đặt tour."));
        checks.add(check(
                "tours_with_photo",
                "Mọi tour đều có ảnh",
                tours.isEmpty() || toursWithPhoto == tours.size(),
                (int) toursWithPhoto,
                tours.size(),
                "Tour không ảnh trông như chưa bán."));

        long dishesPriced = dishes.stream()
                .filter(dish -> dish.getPrice() != null && dish.getPrice().signum() > 0)
                .count();
        checks.add(check(
                "dishes_priced",
                "Mọi món trong thực đơn đều có giá",
                dishes.isEmpty() || dishesPriced == dishes.size(),
                (int) dishesPriced,
                dishes.size(),
                "Món không giá làm khách ngại gọi."));
        return checks;
    }

    /** The share of checks that passed, 0-100 - plain arithmetic, never the model's. */
    static int score(List<AiContentAuditResponse.Check> checks) {
        if (checks.isEmpty()) {
            return 0;
        }
        long passed =
                checks.stream().filter(AiContentAuditResponse.Check::isPassed).count();
        return (int) Math.round(passed * 100.0 / checks.size());
    }

    private JsonNode advise(String model, Hotel hotel, List<AiContentAuditResponse.Check> checks) {
        ObjectNode request = ai.newRequest(model, 1200);
        request.putArray("system").add(ai.textBlock(SYSTEM, false));
        request.putArray("tools").add(tool);
        ObjectNode choice = request.putObject("tool_choice");
        choice.put("type", "tool");
        choice.put("name", "set_content_audit");
        ((ArrayNode) request.get("messages")).add(ai.userMessage(prompt(hotel, checks)));
        JsonNode response = ai.createMessage(request);
        JsonNode call = AiClient.toolUse(response, "set_content_audit");
        if (call != null) {
            return call.path("input");
        }
        JsonNode fromText = ai.jsonObjectIn(AiClient.text(response));
        if (fromText == null) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        return fromText;
    }

    /** The checks as plain lines, with their keys, so the advice can point back at one. */
    static String prompt(Hotel hotel, List<AiContentAuditResponse.Check> checks) {
        StringBuilder prompt = new StringBuilder("Cơ sở: ")
                .append(hotel.getName())
                .append(" - ")
                .append(hotel.getAddress())
                .append("\n\nKẾT QUẢ KIỂM TRA\n");
        for (AiContentAuditResponse.Check check : checks) {
            prompt.append(check.isPassed() ? "- [đạt] " : "- [chưa đạt] ")
                    .append(check.getKey())
                    .append(": ")
                    .append(check.getLabel());
            if (check.getTotal() != null && check.getTotal() > 0) {
                prompt.append(" (")
                        .append(check.getDone())
                        .append("/")
                        .append(check.getTotal())
                        .append(")");
            } else if (check.getDone() != null) {
                prompt.append(" (đang có ").append(check.getDone()).append(")");
            }
            prompt.append("\n");
        }
        return prompt.toString();
    }

    /** Model output → advice that only points at checks that exist and really failed. */
    static AiContentAuditResponse build(
            Long hotelId, int score, List<AiContentAuditResponse.Check> checks, JsonNode advice, List<String> notes) {
        Set<String> failed = new HashSet<>();
        checks.stream().filter(check -> !check.isPassed()).forEach(check -> failed.add(check.getKey()));
        List<AiContentAuditResponse.Item> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int dropped = 0;
        for (JsonNode node : advice.path("items")) {
            String title = text(node.path("title"));
            String how = text(node.path("how"));
            String key = text(node.path("check_key"));
            if (title == null || how == null || items.size() >= 5) {
                continue;
            }
            // Advice about a check that passed (or does not exist) would send the manager chasing nothing.
            if (key != null && !failed.contains(key)) {
                dropped++;
                continue;
            }
            if (key != null && !seen.add(key)) {
                dropped++;
                continue;
            }
            items.add(AiContentAuditResponse.Item.builder()
                    .title(title)
                    .priority(priority(node.path("priority")))
                    .why(text(node.path("why")))
                    .how(how)
                    .checkKey(key)
                    .build());
        }
        if (dropped > 0) {
            notes.add("Đã bỏ " + dropped + " gợi ý không khớp tiêu chí nào đang thiếu.");
        }
        return AiContentAuditResponse.builder()
                .hotelId(hotelId)
                .score(score)
                .summary(text(advice.path("summary")))
                .checks(checks)
                .items(items)
                .notes(notes)
                .build();
    }

    static String priority(JsonNode node) {
        String raw = text(node);
        if (raw == null) {
            return "MEDIUM";
        }
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "HIGH", "LOW" -> raw.toUpperCase(Locale.ROOT);
            default -> "MEDIUM";
        };
    }

    private static AiContentAuditResponse.Check check(
            String key, String label, boolean passed, Integer done, Integer total, String hint) {
        return AiContentAuditResponse.Check.builder()
                .key(key)
                .label(label)
                .passed(passed)
                .done(done)
                .total(total)
                .hint(hint)
                .build();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().strip();
    }
}
