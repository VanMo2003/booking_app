package com.vanmo.booking_app_be.ai;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vanmo.booking_app_be.dto.response.AiHotelCompareResponse;
import com.vanmo.booking_app_be.dto.response.FeedbackRow;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.exception.AppException;
import com.vanmo.booking_app_be.exception.ErrorCode;
import com.vanmo.booking_app_be.repository.AmenityRepository;
import com.vanmo.booking_app_be.repository.FeedbackRepository;
import com.vanmo.booking_app_be.repository.RoomRepository;
import com.vanmo.booking_app_be.repository.ServiceRepository;
import com.vanmo.booking_app_be.repository.TourRepository;

/**
 * Two to four branches side by side - what a guest with a shortlist actually wants, and the one question a star
 * rating never answers: which of these suits me?
 *
 * <p>Every figure in the table is counted here from the branches' own rows (price range, capacity, how many
 * rooms, amenities, services and tours, reviews and their real average), so the comparison cannot be wrong even
 * when the model is. The model is handed the finished table and writes only prose: one line per branch on who it
 * suits, the differences worth knowing, and a verdict. {@link #build} keeps a line only when it names a branch
 * that was actually asked for.
 */
@Service
public class AiHotelCompare {

    static final int MIN_HOTELS = 2;
    static final int MAX_HOTELS = 4;

    private static final String SYSTEM =
            """
			Bạn giúp khách chọn giữa vài cơ sở lưu trú họ đang cân nhắc. Hãy gọi set_hotel_compare.
			Quy tắc:
			- Chỉ dùng "BẢNG SO SÁNH" bên dưới. Không thêm con số, không sửa con số đã cho, không nhắc cơ sở nào ngoài bảng, không lấy kiến thức bên ngoài.
			- fits: mỗi cơ sở trong bảng một dòng, hotel_id đúng như trong bảng, fit_for một câu nói cơ sở đó phù hợp với ai (gia đình, đi công tác, nhóm bạn, khách cần phòng rẻ…), dựa trên đúng số liệu của nó.
			- differences: 2-4 dòng ngắn về khác biệt thật sự đáng để ý (giá, số khách mỗi phòng, tiện ích, tour, đánh giá). Mỗi dòng nói rõ cơ sở nào hơn ở điểm nào. Không nói chung chung.
			- verdict: 2-3 câu tiếng Việt, nói nên chọn cơ sở nào trong hoàn cảnh nào. Không nói cơ sở nào "tốt nhất" một cách chung chung.
			- Cơ sở ít đánh giá thì đừng coi điểm trung bình của nó là chắc chắn.
			- Văn bản thường, không markdown, không emoji.
			""";

    private static final String TOOL =
            """
			{
			"name": "set_hotel_compare",
			"description": "So sánh các cơ sở khách đang cân nhắc.",
			"input_schema": {
				"type": "object",
				"properties": {
				"verdict": {"type": "string"},
				"differences": {"type": "array", "items": {"type": "string"}},
				"fits": {
					"type": "array",
					"items": {
					"type": "object",
					"properties": {
						"hotel_id": {"type": "integer"},
						"fit_for": {"type": "string"}
					},
					"required": ["hotel_id", "fit_for"]
					}
				}
				},
				"required": ["verdict", "differences", "fits"]
			}
			}
			""";

    private final AiClient ai;
    private final AiRateLimiter rateLimiter;
    private final HotelKnowledge knowledge;
    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final ServiceRepository serviceRepository;
    private final TourRepository tourRepository;
    private final FeedbackRepository feedbackRepository;
    private final JsonNode tool;

    public AiHotelCompare(
            AiClient ai,
            AiRateLimiter rateLimiter,
            HotelKnowledge knowledge,
            RoomRepository roomRepository,
            AmenityRepository amenityRepository,
            ServiceRepository serviceRepository,
            TourRepository tourRepository,
            FeedbackRepository feedbackRepository)
            throws JsonProcessingException {
        this.ai = ai;
        this.rateLimiter = rateLimiter;
        this.knowledge = knowledge;
        this.roomRepository = roomRepository;
        this.amenityRepository = amenityRepository;
        this.serviceRepository = serviceRepository;
        this.tourRepository = tourRepository;
        this.feedbackRepository = feedbackRepository;
        this.tool = ai.json().readTree(TOOL);
    }

    @Transactional(readOnly = true)
    public AiHotelCompareResponse compare(List<Long> rawIds, String clientKey) {
        List<Long> ids = ids(rawIds);
        if (!ai.isEnabled()) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        rateLimiter.check("hotel-compare:" + clientKey, 10, Duration.ofMinutes(10));

        List<AiHotelCompareResponse.Item> items = new ArrayList<>();
        for (Long hotelId : ids) {
            items.add(item(knowledge.hotel(hotelId)));
        }
        String prompt = prompt(items);
        JsonNode compared = ai.withModelFallback(ai.getAssistantModels(), model -> write(model, prompt));
        return build(items, compared, new ArrayList<>());
    }

    /** The ids to compare: at least two different branches, at most {@link #MAX_HOTELS}, duplicates dropped. */
    static List<Long> ids(List<Long> rawIds) {
        Set<Long> unique = new LinkedHashSet<>();
        if (rawIds != null) {
            rawIds.stream().filter(id -> id != null).forEach(unique::add);
        }
        if (unique.size() < MIN_HOTELS) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Hãy chọn ít nhất " + MIN_HOTELS + " cơ sở khác nhau");
        }
        if (unique.size() > MAX_HOTELS) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chỉ so sánh được tối đa " + MAX_HOTELS + " cơ sở");
        }
        return List.copyOf(unique);
    }

    /** One branch's row, counted from its own rows - never from the stored rating or anything the model says. */
    private AiHotelCompareResponse.Item item(Hotel hotel) {
        List<Room> rooms = roomRepository.findAllByHotel_Id(hotel.getId());
        List<FeedbackRow> reviews = feedbackRepository.findByHotel(hotel.getId());
        ReviewDigest.Stats stats = ReviewDigest.stats(reviews);
        List<BigDecimal> prices = rooms.stream()
                .map(Room::getPrice)
                .filter(price -> price != null && price.signum() > 0)
                .sorted(Comparator.naturalOrder())
                .toList();
        return AiHotelCompareResponse.Item.builder()
                .hotelId(hotel.getId())
                .name(hotel.getName())
                .category(hotel.getCategory())
                .address(hotel.getAddress())
                .priceFrom(prices.isEmpty() ? null : prices.getFirst())
                .priceTo(prices.isEmpty() ? null : prices.getLast())
                .maxCapacity(rooms.stream()
                        .map(Room::getCapacity)
                        .filter(capacity -> capacity != null)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0))
                .rooms(rooms.size())
                .amenities(amenityRepository.findAllByHotel_Id(hotel.getId()).size())
                .services(serviceRepository.findAllByHotel_Id(hotel.getId()).size())
                .tours(tourRepository.findAllByHotel_Id(hotel.getId()).size())
                .reviews(stats.count())
                .averageRating(stats.average())
                .build();
    }

    /** The table as plain lines, with each branch's id so a line can point back at one. */
    static String prompt(List<AiHotelCompareResponse.Item> items) {
        StringBuilder prompt = new StringBuilder("BẢNG SO SÁNH (số liệu hệ thống)\n");
        for (AiHotelCompareResponse.Item item : items) {
            prompt.append("- [mã ")
                    .append(item.getHotelId())
                    .append("] ")
                    .append(item.getName())
                    .append(" · ")
                    .append(item.getCategory() == null ? "(chưa rõ loại hình)" : item.getCategory())
                    .append(" · ")
                    .append(item.getAddress())
                    .append("\n  Giá phòng: ")
                    .append(
                            item.getPriceFrom() == null
                                    ? "(chưa có phòng nào có giá)"
                                    : HotelKnowledge.money(item.getPriceFrom()) + " – "
                                            + HotelKnowledge.money(item.getPriceTo()))
                    .append("; ")
                    .append(item.getRooms())
                    .append(" phòng, phòng lớn nhất ")
                    .append(item.getMaxCapacity())
                    .append(" khách")
                    .append("\n  Tiện ích: ")
                    .append(item.getAmenities())
                    .append("; dịch vụ: ")
                    .append(item.getServices())
                    .append("; tour: ")
                    .append(item.getTours())
                    .append("\n  Đánh giá: ")
                    .append(
                            item.getReviews() == 0
                                    ? "chưa có đánh giá"
                                    : item.getReviews() + " đánh giá, trung bình " + item.getAverageRating() + "/5")
                    .append("\n");
        }
        return prompt.toString();
    }

    private JsonNode write(String model, String prompt) {
        ObjectNode request = ai.newRequest(model, 1000);
        request.putArray("system").add(ai.textBlock(SYSTEM, false));
        request.putArray("tools").add(tool);
        ObjectNode choice = request.putObject("tool_choice");
        choice.put("type", "tool");
        choice.put("name", "set_hotel_compare");
        ((ArrayNode) request.get("messages")).add(ai.userMessage(prompt));
        JsonNode response = ai.createMessage(request);
        JsonNode call = AiClient.toolUse(response, "set_hotel_compare");
        if (call != null) {
            return call.path("input");
        }
        JsonNode fromText = ai.jsonObjectIn(AiClient.text(response));
        if (fromText == null) {
            throw new AppException(ErrorCode.AI_UNAVAILABLE);
        }
        return fromText;
    }

    /** Model output → the table with each branch's line filled in, dropping lines about a branch nobody asked for. */
    static AiHotelCompareResponse build(
            List<AiHotelCompareResponse.Item> items, JsonNode compared, List<String> notes) {
        Map<Long, String> fits = new HashMap<>();
        Set<Long> known = new HashSet<>();
        items.forEach(item -> known.add(item.getHotelId()));
        int dropped = 0;
        for (JsonNode node : compared.path("fits")) {
            long hotelId = node.path("hotel_id").asLong(0);
            String fitFor = text(node.path("fit_for"));
            if (fitFor == null || !known.contains(hotelId) || fits.containsKey(hotelId)) {
                dropped++;
                continue;
            }
            fits.put(hotelId, fitFor);
        }
        if (dropped > 0) {
            notes.add("Đã bỏ " + dropped + " nhận xét không thuộc cơ sở nào đang so sánh.");
        }
        items.forEach(item -> item.setFitFor(fits.get(item.getHotelId())));

        List<String> differences = new ArrayList<>();
        for (JsonNode node : compared.path("differences")) {
            String line = text(node);
            if (line != null && !differences.contains(line) && differences.size() < 4) {
                differences.add(line);
            }
        }
        return AiHotelCompareResponse.builder()
                .items(items)
                .differences(differences)
                .verdict(text(compared.path("verdict")))
                .notes(notes)
                .build();
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().strip();
    }
}
