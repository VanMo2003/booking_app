package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiTripPlanResponse;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.dto.response.VoucherResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.enums.DiscountType;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

/**
 * The pure halves of the trip planner: the sentence → a usable trip request, and the model's plan → a plan that only
 * uses real rows, with the real sums. No model and no database.
 */
class AiTripPlannerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static Hotel hotel(long id, String name) {
        return Hotel.builder()
                .id(id)
                .name(name)
                .address("12 Bãi Cháy, Hạ Long")
                .category("Grand")
                .rating(4.5)
                .build();
    }

    private static AiTripPlanner.StayOption stay(long id, String name, String price, boolean sleepsEveryone) {
        return new AiTripPlanner.StayOption(
                hotel(id, name),
                new HotelKnowledge.RoomOption(90L, "101", "Deluxe", 3, new BigDecimal(price), List.of()),
                sleepsEveryone);
    }

    private static TourResponse tour(long id, String name, String adult, String child) {
        return TourResponse.builder()
                .id(id)
                .name(name)
                .price(new BigDecimal(adult))
                .salePrice(new BigDecimal(adult))
                .saleChildPrice(child == null ? null : new BigDecimal(child))
                .childPrice(child == null ? null : new BigDecimal(child))
                .available(true)
                .build();
    }

    private static AiTripPlanner.Request request(int nights, int adults, int children, String budget) {
        return new AiTripPlanner.Request(
                "Hạ Long",
                List.of("Hạ Long"),
                LocalDate.of(2026, 12, 5),
                nights,
                adults,
                children,
                budget == null ? null : new BigDecimal(budget),
                Set.of(TravelTheme.BEACH),
                "Hạ Long 3 ngày 2 đêm");
    }

    @Test
    void readsTheTripAndFillsInWhatIsMissing() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiTripPlanner.Request request = AiTripPlanner.request(
                input(
                        """
				{
				"destination": "Hạ Long",
				"destinations": ["Quảng Ninh"],
				"nights": 2,
				"start_date": "2026-12-05",
				"adults": 2,
				"children": 1,
				"budget": 6000000,
				"themes": ["BEACH", "FOOD", "SPACE"],
				"summary": "Hạ Long 3 ngày 2 đêm cho gia đình"
				}
				"""),
                TODAY,
                notes);

        assertThat(request.destination()).isEqualTo("Hạ Long");
        assertThat(request.destinations()).containsExactly("Hạ Long", "Quảng Ninh");
        assertThat(request.nights()).isEqualTo(2);
        assertThat(request.guests()).isEqualTo(3);
        assertThat(request.budget()).isEqualByComparingTo("6000000");
        assertThat(request.themes()).containsExactlyInAnyOrder(TravelTheme.BEACH, TravelTheme.FOOD);
        assertThat(notes).isEmpty();
    }

    @Test
    void aDayThatHasPassedBecomesTomorrowWithANote() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiTripPlanner.Request request = AiTripPlanner.request(
                input("""
				{"start_date": "2026-11-01", "summary": "Đi chơi"}
				"""), TODAY, notes);

        assertThat(request.startDate()).isEqualTo(TODAY.plusDays(1));
        // One night, two adults, no budget: safe defaults rather than a failed request.
        assertThat(request.nights()).isEqualTo(1);
        assertThat(request.adults()).isEqualTo(2);
        assertThat(request.children()).isZero();
        assertThat(request.budget()).isNull();
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("đã qua"));
    }

    @Test
    void keepsOnlyRealRowsAndSumsThem() throws JsonProcessingException {
        AiTripPlanner.Candidates candidates = new AiTripPlanner.Candidates(
                List.of(stay(6, "Hạ Long Bay Hotel", "900000", true)),
                List.of(tour(7, "Du thuyền vịnh Hạ Long", "500000", "300000")),
                2);
        List<String> notes = new ArrayList<>();

        AiTripPlanResponse plan = AiTripPlanner.build(
                "Hạ Long 3 ngày 2 đêm",
                request(2, 2, 1, "6000000"),
                candidates,
                input(
                        """
				{
				"summary": "Hạ Long 3 ngày 2 đêm nghiêng về biển",
				"hotel_id": 6,
				"days": [
					{"index": 1, "title": "Nhận phòng và dạo Bãi Cháy", "steps": [
					{"time": "14:00", "title": "Nhận phòng", "detail": "Gửi hành lý rồi nghỉ ngơi."}
					]},
					{"index": 2, "title": "Du thuyền vịnh", "steps": [
					{"time": "8h", "title": "Du thuyền vịnh Hạ Long", "tour_id": 7},
					{"time": "19:00", "title": "Tour đảo Tuần Châu", "tour_id": 999}
					]},
					{"index": 3, "title": "Trả phòng", "steps": [{"time": "12:00", "title": "Trả phòng"}]},
					{"index": 4, "title": "Ngày thừa", "steps": [{"title": "Không nên có"}]}
				]
				}
				"""),
                notes,
                hotelId -> List.of());

        assertThat(plan.getStay().getHotelId()).isEqualTo(6);
        assertThat(plan.getStay().getCheckin()).isEqualTo(LocalDate.of(2026, 12, 5));
        assertThat(plan.getStay().getCheckout()).isEqualTo(LocalDate.of(2026, 12, 7));
        // 3 ngày 2 đêm: the fourth day the model added is cut.
        assertThat(plan.getDays()).hasSize(3);
        assertThat(plan.getDays().get(1).getDate()).isEqualTo(LocalDate.of(2026, 12, 6));
        assertThat(plan.getDays().get(1).getSteps()).hasSize(1);
        // "8h" is read as a time, and the invented tour is gone.
        assertThat(plan.getDays().get(1).getSteps().getFirst().getTime()).isEqualTo("08:00");
        assertThat(plan.getTours()).hasSize(1);
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("Đã bỏ 1 hoạt động"));
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("2 tour gói kèm phòng"));

        // 900.000 × 2 đêm + (500.000 × 2 + 300.000 × 1)
        assertThat(plan.getCost().getRooms()).isEqualByComparingTo("1800000");
        assertThat(plan.getCost().getTours()).isEqualByComparingTo("1300000");
        assertThat(plan.getCost().getTotal()).isEqualByComparingTo("3100000");
        assertThat(plan.getCost().getPerPerson()).isEqualByComparingTo("1033333");
        assertThat(plan.getCost().getLeftOver()).isEqualByComparingTo("2900000");
    }

    @Test
    void aHotelOutsideTheListIsReplacedAndTheBudgetIsChecked() throws JsonProcessingException {
        AiTripPlanner.Candidates candidates =
                new AiTripPlanner.Candidates(List.of(stay(6, "Hạ Long Bay Hotel", "900000", false)), List.of(), 0);
        List<String> notes = new ArrayList<>();

        AiTripPlanResponse plan = AiTripPlanner.build(
                "Hạ Long 2 đêm",
                request(2, 2, 0, "1000000"),
                candidates,
                input(
                        """
				{"summary": "Chuyến đi", "hotel_id": 404, "days": [
					{"index": 1, "title": "Ngày 1", "steps": [{"time": "14:00", "title": "Nhận phòng"}]}
				]}
				"""),
                notes,
                hotelId -> List.of());

        assertThat(plan.getStay().getHotelId()).isEqualTo(6);
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("ngoài danh sách"));
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("đủ chỗ cho cả nhóm"));
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("vượt ngân sách"));
        assertThat(plan.getCost().getLeftOver()).isEqualByComparingTo("-800000");
    }

    @Test
    void picksTheVoucherThatTakesMostOffTheTourPart() {
        VoucherResponse percent = VoucherResponse.builder()
                .code("CHAOMUNG")
                .discountType(DiscountType.PERCENT)
                .value(new BigDecimal("10"))
                .maxDiscount(new BigDecimal("200000"))
                .build();
        VoucherResponse fixed = VoucherResponse.builder()
                .code("HALONG200")
                .discountType(DiscountType.AMOUNT)
                .value(new BigDecimal("200000"))
                .minOrderAmount(new BigDecimal("2000000"))
                .build();

        // 1.000.000: 10% = 100.000 wins, the fixed one needs a bigger order.
        assertThat(AiTripPlanner.bestVoucher(List.of(percent, fixed), new BigDecimal("1000000")))
                .isSameAs(percent);
        // 3.000.000: the percent is capped at 200.000, so they tie and the first one found stays.
        assertThat(AiTripPlanner.bestVoucher(List.of(fixed, percent), new BigDecimal("3000000")))
                .isSameAs(fixed);
        // No tours in the plan: no voucher.
        assertThat(AiTripPlanner.bestVoucher(List.of(percent), BigDecimal.ZERO)).isNull();
    }

    @Test
    void childrenPayTheAdultPriceWhenTheTourHasNoChildPrice() {
        assertThat(AiTripPlanner.tourAmount(tour(7, "Food tour", "400000", null), 2, 2))
                .isEqualByComparingTo("1600000");
        assertThat(AiTripPlanner.tourAmount(tour(7, "Food tour", "400000", "250000"), 2, 2))
                .isEqualByComparingTo("1300000");
    }
}
