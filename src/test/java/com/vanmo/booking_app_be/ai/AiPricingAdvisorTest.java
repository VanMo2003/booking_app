package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiPricingAdviceResponse;
import com.vanmo.booking_app_be.dto.response.RevenueByRoomTypeDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.WeekdayStatsDTO;

/** The figures the pricing advisor sends, and how it checks the advice that comes back. */
class AiPricingAdvisorTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate FROM = LocalDate.of(2026, 10, 18);
    private static final LocalDate TO = LocalDate.of(2026, 11, 16);

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static RevenueByRoomTypeDTO type(long id, String name, String listPrice, String adr, double occupancy) {
        RevenueByRoomTypeDTO type = new RevenueByRoomTypeDTO();
        type.setRoomTypeId(id);
        type.setRoomTypeName(name);
        type.setListPrice(new BigDecimal(listPrice));
        type.setAdr(new BigDecimal(adr));
        type.setOccupancyRate(occupancy);
        type.setRooms(5);
        type.setRoomNights(40);
        type.setTotalRevenue(new BigDecimal("34000000"));
        return type;
    }

    @Test
    void theBriefCarriesEveryRoomTypeAndTheWeekdayPattern() {
        RevenueSummaryResponse summary = new RevenueSummaryResponse();
        RevenuePeriodDTO period = new RevenuePeriodDTO();
        period.setOccupancyRate(78.0);
        period.setAdr(new BigDecimal("860000"));
        period.setRevpar(new BigDecimal("670000"));
        period.setRoomNightsSold(117);
        period.setAvailableRoomNights(150);
        period.setCancellationRate(4.5);
        summary.setCurrent(period);

        WeekdayStatsDTO sunday = new WeekdayStatsDTO();
        sunday.setDayOfWeek(7);
        sunday.setAverageOccupancy(35.0);
        sunday.setAverageRevenue(new BigDecimal("3200000"));

        RevenueOutlookResponse outlook = new RevenueOutlookResponse();
        outlook.setBookedRoomNights(42);
        outlook.setAvailableRoomNights(150);
        outlook.setOccupancyOnTheBooks(28.0);
        outlook.setTotalExpected(new BigDecimal("41000000"));

        String brief = AiPricingAdvisor.brief(
                FROM,
                TO,
                summary,
                List.of(type(1, "Deluxe", "900000", "870000", 82.0), type(2, "Standard", "600000", "450000", 31.0)),
                List.of(sunday),
                outlook);

        assertThat(brief).startsWith("SỐ LIỆU");
        assertThat(brief).contains("lấp phòng 78,0%, ADR 860.000 ₫");
        assertThat(brief).contains("- Deluxe: 5 phòng, giá niêm yết 900.000 ₫, ADR thực 870.000 ₫, lấp phòng 82,0%");
        assertThat(brief).contains("- Standard:");
        assertThat(brief).contains("Chủ Nhật: 35,0%");
        assertThat(brief).contains("Đã đặt trước cho 30 ngày tới: 42/150 đêm phòng");
    }

    @Test
    void keepsOnlyRoomTypesThatExistAndWorksOutThePriceItself() throws JsonProcessingException {
        List<RevenueByRoomTypeDTO> types =
                List.of(type(1, "Deluxe", "900000", "870000", 82.0), type(2, "Standard", "600000", "450000", 31.0));
        List<String> notes = new ArrayList<>();

        AiPricingAdviceResponse advice = AiPricingAdvisor.build(
                6L,
                FROM,
                TO,
                types,
                input(
                        """
				{
				"summary": "Deluxe gần kín, Standard còn trống nhiều.",
				"items": [
					{"room_type": "deluxe", "action": "RAISE", "percent": 8, "reason": "Lấp phòng 82,0% và ADR sát giá niêm yết."},
					{"room_type": "Tổng thống", "action": "RAISE", "percent": 50, "reason": "Loại phòng không có thật."}
				],
				"risks": ["Tăng giá có thể làm giảm lượt đặt cuối tuần."]
				}
				"""),
                notes);

        assertThat(advice.getItems()).hasSize(2);
        AiPricingAdviceResponse.Item deluxe = advice.getItems().getFirst();
        // Matched without the accent and the capital letter.
        assertThat(deluxe.getRoomTypeName()).isEqualTo("Deluxe");
        assertThat(deluxe.getAction()).isEqualTo("RAISE");
        assertThat(deluxe.getPercent()).isEqualTo(8);
        // 900.000 + 8% = 972.000, rounded to the nearest thousand.
        assertThat(deluxe.getSuggestedPrice()).isEqualByComparingTo("972000");
        // A type the advice skipped is still listed, as "giữ nguyên".
        AiPricingAdviceResponse.Item standard = advice.getItems().getLast();
        assertThat(standard.getRoomTypeName()).isEqualTo("Standard");
        assertThat(standard.getAction()).isEqualTo("HOLD");
        assertThat(standard.getSuggestedPrice()).isNull();
        assertThat(advice.getRisks()).hasSize(1);
        assertThat(notes).anySatisfy(note -> assertThat(note).contains("Đã bỏ 1 gợi ý"));
    }

    @Test
    void aPercentageOutsideWhatIsAllowedIsClamped() throws JsonProcessingException {
        assertThat(AiPricingAdvisor.percent(input("45"), "RAISE")).isEqualTo(AiPricingAdvisor.MAX_PERCENT);
        assertThat(AiPricingAdvisor.percent(input("-10"), "LOWER")).isEqualTo(10);
        assertThat(AiPricingAdvisor.percent(input("0"), "RAISE")).isNull();
        assertThat(AiPricingAdvisor.percent(input("8"), "HOLD")).isNull();
        assertThat(AiPricingAdvisor.percent(input("\"nhiều\""), "RAISE")).isNull();
    }

    @Test
    void anUnknownActionBecomesHold() throws JsonProcessingException {
        assertThat(AiPricingAdvisor.action(input("\"raise\""))).isEqualTo("RAISE");
        assertThat(AiPricingAdvisor.action(input("\"tăng giá\""))).isEqualTo("HOLD");
        assertThat(AiPricingAdvisor.action(input("null"))).isEqualTo("HOLD");
    }

    @Test
    void aLowerSuggestionTakesThePriceDown() {
        assertThat(AiPricingAdvisor.suggestedPrice(new BigDecimal("600000"), "LOWER", 10))
                .isEqualByComparingTo("540000");
        assertThat(AiPricingAdvisor.suggestedPrice(new BigDecimal("600000"), "HOLD", null))
                .isNull();
        // A type nobody ever priced cannot be moved.
        assertThat(AiPricingAdvisor.suggestedPrice(BigDecimal.ZERO, "RAISE", 10))
                .isNull();
    }
}
