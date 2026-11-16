package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiContentAuditResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.ItineraryStep;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.Tour;

/** The checks and the score are counted here, so they are what the test pins down. */
class AiContentAuditTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static Hotel hotel(String description) {
        return Hotel.builder()
                .id(6L)
                .name("Hoàn Kiếm Palace")
                .address("12 Hàng Bạc, Hà Nội")
                .description(description)
                .build();
    }

    private static Room room(String photo, String price) {
        Room room = new Room();
        room.setRoomNumber("101");
        room.setPathImage(photo);
        room.setPrice(new BigDecimal(price));
        return room;
    }

    private static Tour tour(String destination, boolean itinerary, String photo) {
        Tour tour = new Tour();
        tour.setName("Food tour");
        tour.setDestination(destination);
        tour.setPathImage(photo);
        if (itinerary) {
            tour.setItinerary(new ArrayList<>(List.of(new ItineraryStep("18:00", "Khởi hành", null))));
        }
        return tour;
    }

    private static AiContentAuditResponse.Check find(List<AiContentAuditResponse.Check> checks, String key) {
        return checks.stream()
                .filter(check -> check.getKey().equals(key))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void aFullyFilledBranchPassesEverything() {
        List<AiContentAuditResponse.Check> checks = AiContentAudit.checks(
                hotel("Khách sạn nằm ngay phố cổ Hà Nội, đi bộ ra Hồ Gươm chỉ vài phút, có nhà hàng và quầy bar"
                        + " phục vụ cả ngày cho khách nghỉ dưỡng và công tác."),
                List.of(room("/uploads/a.png", "900000")),
                List.of(tour("Phố cổ Hà Nội", true, "/uploads/t.png")),
                List.of(),
                2,
                5,
                4);

        assertThat(checks).allSatisfy(check -> assertThat(check.isPassed()).isTrue());
        assertThat(AiContentAudit.score(checks)).isEqualTo(100);
    }

    @Test
    void countsWhatIsMissingRowByRow() {
        List<AiContentAuditResponse.Check> checks = AiContentAudit.checks(
                hotel("Ngắn quá"),
                List.of(room("/uploads/a.png", "900000"), room(null, "0")),
                List.of(tour(null, false, null)),
                List.of(),
                0,
                0,
                0);

        assertThat(find(checks, "hotel_description").isPassed()).isFalse();
        assertThat(find(checks, "hotel_photos").isPassed()).isFalse();
        assertThat(find(checks, "hotel_amenities").isPassed()).isFalse();
        assertThat(find(checks, "hotel_services").isPassed()).isFalse();
        assertThat(find(checks, "rooms_exist").isPassed()).isTrue();

        AiContentAuditResponse.Check photos = find(checks, "rooms_with_photo");
        assertThat(photos.isPassed()).isFalse();
        assertThat(photos.getDone()).isEqualTo(1);
        assertThat(photos.getTotal()).isEqualTo(2);

        assertThat(find(checks, "rooms_priced").isPassed()).isFalse();
        assertThat(find(checks, "tours_with_destination").isPassed()).isFalse();
        assertThat(find(checks, "tours_with_itinerary").isPassed()).isFalse();
        assertThat(find(checks, "tours_with_photo").isPassed()).isFalse();
        // No dish at all: nothing to nag about.
        assertThat(find(checks, "dishes_priced").isPassed()).isTrue();
        // 2 trong 11 tiêu chí đạt: có phòng, và thực đơn rỗng nên không bị trừ.
        assertThat(AiContentAudit.score(checks)).isEqualTo(18);
    }

    @Test
    void aBranchThatSellsNoTourIsNotNaggedAboutTours() {
        List<AiContentAuditResponse.Check> checks = AiContentAudit.checks(
                hotel("Mô tả đủ dài ".repeat(12)),
                List.of(room("/uploads/a.png", "900000")),
                List.of(),
                List.of(),
                1,
                1,
                3);

        assertThat(find(checks, "tours_with_itinerary").isPassed()).isTrue();
        assertThat(find(checks, "tours_with_itinerary").getTotal()).isZero();
    }

    @Test
    void adviceAboutACheckThatPassedIsDropped() throws JsonProcessingException {
        List<AiContentAuditResponse.Check> checks = AiContentAudit.checks(
                hotel("Ngắn"), List.of(room("/uploads/a.png", "900000")), List.of(), List.of(), 1, 1, 3);
        List<String> notes = new ArrayList<>();

        AiContentAuditResponse audit = AiContentAudit.build(
                6L,
                AiContentAudit.score(checks),
                checks,
                input(
                        """
				{
				"summary": "Trang còn thiếu mô tả.",
				"items": [
					{"title": "Viết mô tả cho cơ sở", "priority": "HIGH", "why": "Khách không biết cơ sở có gì.",
					"how": "Viết 4-5 câu về vị trí và tiện ích.", "check_key": "hotel_description"},
					{"title": "Thêm ảnh phòng", "priority": "cao", "why": "", "how": "Tải ảnh lên.",
					"check_key": "rooms_with_photo"},
					{"title": "Thêm mô tả lần hai", "priority": "LOW", "how": "Trùng tiêu chí.",
					"check_key": "hotel_description"}
				]
				}
				"""),
                notes);

        assertThat(audit.getScore()).isEqualTo(AiContentAudit.score(checks));
        // rooms_with_photo passed here, and the repeated key is dropped too.
        assertThat(audit.getItems()).hasSize(1);
        assertThat(audit.getItems().getFirst().getCheckKey()).isEqualTo("hotel_description");
        assertThat(audit.getItems().getFirst().getPriority()).isEqualTo("HIGH");
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("Đã bỏ 2 gợi ý"));
    }

    @Test
    void anUnknownPriorityBecomesMedium() throws JsonProcessingException {
        assertThat(AiContentAudit.priority(input("\"high\""))).isEqualTo("HIGH");
        assertThat(AiContentAudit.priority(input("\"gấp\""))).isEqualTo("MEDIUM");
        assertThat(AiContentAudit.priority(input("null"))).isEqualTo("MEDIUM");
    }
}
