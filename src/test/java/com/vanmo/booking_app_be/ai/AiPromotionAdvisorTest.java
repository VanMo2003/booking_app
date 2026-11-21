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
import com.vanmo.booking_app_be.dto.response.AiPromotionAdviceResponse;
import com.vanmo.booking_app_be.dto.response.BranchPerformanceDTO;
import com.vanmo.booking_app_be.dto.response.RevenueByTourDTO;
import com.vanmo.booking_app_be.dto.response.RevenueOutlookResponse;
import com.vanmo.booking_app_be.dto.response.RevenuePeriodDTO;
import com.vanmo.booking_app_be.dto.response.RevenueSummaryResponse;
import com.vanmo.booking_app_be.dto.response.WeekdayStatsDTO;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Voucher;

/** The model proposes campaigns; the percentage, the dates, the minimum order and the branch are all checked here. */
class AiPromotionAdvisorTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate TO = LocalDate.of(2026, 11, 18);
    private static final LocalDate FROM = TO.minusDays(29);

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static RevenueSummaryResponse summary(String adr, double occupancy) {
        RevenuePeriodDTO period = RevenuePeriodDTO.builder()
                .from(FROM)
                .to(TO)
                .totalRevenue(new BigDecimal("120000000"))
                .adr(adr == null ? null : new BigDecimal(adr))
                .occupancyRate(occupancy)
                .completedBookings(42)
                .completedTours(6)
                .build();
        RevenueSummaryResponse response = new RevenueSummaryResponse();
        response.setCurrent(period);
        return response;
    }

    private static List<Hotel> chain() {
        return List.of(
                Hotel.builder().id(6L).name("Hoàn Kiếm Palace").build(),
                Hotel.builder().id(7L).name("Ba Đình Riverside").build());
    }

    @Test
    void phan_tram_va_so_ngay_khong_bao_gio_la_so_cua_model() throws JsonProcessingException {
        JsonNode node = input(
                """
				{"deep": 60, "shallow": 1, "ok": 15, "d7": 7, "d30": 30, "d9": 9, "text": "mười"}
				""");

        // Giảm quá sâu hoặc quá nông đều bị kéo về hai đầu khoảng 5-25.
        assertThat(AiPromotionAdvisor.percent(node.path("deep"))).isEqualTo(25);
        assertThat(AiPromotionAdvisor.percent(node.path("shallow"))).isEqualTo(5);
        assertThat(AiPromotionAdvisor.percent(node.path("ok"))).isEqualTo(15);
        assertThat(AiPromotionAdvisor.percent(node.path("missing"))).isEqualTo(5);

        // Chỉ ba mốc 7/14/30; mốc lạ thành 14 nên ngày kết thúc luôn do backend tính.
        assertThat(AiPromotionAdvisor.horizon(node.path("d7"))).isEqualTo(7);
        assertThat(AiPromotionAdvisor.horizon(node.path("d30"))).isEqualTo(30);
        assertThat(AiPromotionAdvisor.horizon(node.path("d9"))).isEqualTo(14);
        assertThat(AiPromotionAdvisor.horizon(node.path("text"))).isEqualTo(14);
    }

    @Test
    void don_toi_thieu_lam_tron_tu_adr_that() {
        assertThat(AiPromotionAdvisor.minOrder(summary("843000", 61).getCurrent()))
                .isEqualByComparingTo("840000");
        assertThat(AiPromotionAdvisor.minOrder(summary("846000", 61).getCurrent()))
                .isEqualByComparingTo("850000");
        // Chưa có ADR thì không áp mức tối thiểu nào.
        assertThat(AiPromotionAdvisor.minOrder(summary(null, 0).getCurrent())).isNull();
        assertThat(AiPromotionAdvisor.minOrder(null)).isNull();
    }

    @Test
    void ma_dang_chay_doc_theo_ngay_hom_nay() {
        Voucher running = Voucher.builder()
                .code("GIAMHE10")
                .title("Giảm hè")
                .startsAt(TO.minusDays(5))
                .endsAt(TO.plusDays(5))
                .active(true)
                .build();
        Voucher expired = Voucher.builder()
                .code("CU")
                .title("Đã hết")
                .startsAt(TO.minusDays(30))
                .endsAt(TO.minusDays(1))
                .active(true)
                .build();
        Voucher paused = Voucher.builder()
                .code("TAT")
                .title("Đang tắt")
                .startsAt(TO.minusDays(1))
                .endsAt(TO.plusDays(1))
                .active(false)
                .build();

        assertThat(AiPromotionAdvisor.isRunning(running, TO)).isTrue();
        assertThat(AiPromotionAdvisor.isRunning(expired, TO)).isFalse();
        assertThat(AiPromotionAdvisor.isRunning(paused, TO)).isFalse();
        assertThat(AiPromotionAdvisor.label(running)).isEqualTo("GIAMHE10 - Giảm hè (toàn chuỗi, đến 2026-11-23)");
    }

    @Test
    void brief_neu_cho_trong_theo_co_so_ngay_trong_tuan_va_tour() {
        String brief = AiPromotionAdvisor.brief(
                FROM,
                TO,
                summary("843000", 61.4),
                List.of(BranchPerformanceDTO.builder()
                        .hotelId(7L)
                        .hotelName("Ba Đình Riverside")
                        .current(RevenuePeriodDTO.builder()
                                .totalRevenue(new BigDecimal("20000000"))
                                .occupancyRate(38.2)
                                .build())
                        .revenueShare(16.7)
                        .revenueGrowth(-12.5)
                        .build()),
                List.of(WeekdayStatsDTO.builder()
                        .dayOfWeek(3)
                        .averageOccupancy(31.0)
                        .averageRevenue(new BigDecimal("2000000"))
                        .build()),
                List.of(RevenueByTourDTO.builder()
                        .tourName("Food tour phố cổ")
                        .orders(2)
                        .guests(5)
                        .tourRevenue(new BigDecimal("2250000"))
                        .seatsOffered(40)
                        .fillRate(12.5)
                        .build()),
                RevenueOutlookResponse.builder()
                        .occupancyOnTheBooks(22.4)
                        .totalExpected(new BigDecimal("18000000"))
                        .bookings(9)
                        .tours(1)
                        .build(),
                List.of("GIAMHE10 - Giảm hè (toàn chuỗi, đến 2026-11-23)"));

        assertThat(brief)
                .contains("Ba Đình Riverside")
                .contains("16,7% của chuỗi")
                .contains("-12,5%");
        assertThat(brief).contains("Thứ ba: lấp phòng 31,0%");
        assertThat(brief).contains("Food tour phố cổ").contains("lấp chỗ 12,5%");
        assertThat(brief).contains("30 ngày tới đã đặt trước: lấp phòng 22,4%");
        assertThat(brief).contains("GIAMHE10");
    }

    @Test
    void chi_giu_chuong_trinh_tro_dung_co_so_cua_chuoi_va_tour_that() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiPromotionAdviceResponse advice = AiPromotionAdvisor.build(
                3L,
                FROM,
                TO,
                summary("843000", 61.4),
                RevenueOutlookResponse.builder().occupancyOnTheBooks(22.4).build(),
                chain(),
                Set.of("Food tour phố cổ"),
                List.of(),
                input(
                        """
						{
						"summary": "Giữa tuần còn trống nhiều.",
						"campaigns": [
							{"title": "Giảm 10% đêm giữa tuần", "scope": "CHAIN", "discount_percent": 10,
							"horizon_days": 14, "why": "Thứ ba chỉ lấp 31% phòng.",
							"tours": ["Food tour phố cổ", "Tour không có thật", "Food tour phố cổ"]},
							{"title": "Giảm sâu cho Ba Đình", "scope": "BRANCH", "hotel_name": "ba đình riverside",
							"discount_percent": 60, "horizon_days": 9, "why": "Cơ sở này hụt 12,5%."},
							{"title": "Giảm cho cơ sở Đà Nẵng", "scope": "BRANCH", "hotel_name": "Đà Nẵng Bay",
							"discount_percent": 15, "horizon_days": 7, "why": "Cơ sở ngoài chuỗi."},
							{"title": "Thiếu lý do", "scope": "CHAIN", "discount_percent": 10, "horizon_days": 7}
						]
						}
						"""),
                notes);

        assertThat(advice.getCampaigns()).hasSize(2);

        AiPromotionAdviceResponse.Campaign chainWide = advice.getCampaigns().getFirst();
        assertThat(chainWide.getScope()).isEqualTo("CHAIN");
        assertThat(chainWide.getHotelId()).isNull();
        assertThat(chainWide.getStartsAt()).isEqualTo(TO);
        assertThat(chainWide.getEndsAt()).isEqualTo(TO.plusDays(13));
        assertThat(chainWide.getMinOrderAmount()).isEqualByComparingTo("840000");
        // Tour không có thật bị bỏ, tour trùng chỉ còn một lần.
        assertThat(chainWide.getTours()).containsExactly("Food tour phố cổ");

        AiPromotionAdviceResponse.Campaign branch = advice.getCampaigns().get(1);
        assertThat(branch.getHotelId()).isEqualTo(7L);
        assertThat(branch.getHotelName()).isEqualTo("Ba Đình Riverside");
        // Giảm 60% bị kéo về mức tối đa, mốc 9 ngày thành 14 ngày.
        assertThat(branch.getDiscountPercent()).isEqualTo(AiPromotionAdvisor.MAX_PERCENT);
        assertThat(branch.getEndsAt()).isEqualTo(TO.plusDays(13));

        assertThat(advice.getOccupancyRate()).isEqualTo(61.4);
        assertThat(notes).hasSize(1);
        assertThat(notes.getFirst()).contains("2 chương trình");
    }

    @Test
    void khong_co_chuong_trinh_nao_thi_noi_ro_la_chua_can_giam_gia() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiPromotionAdviceResponse advice = AiPromotionAdvisor.build(
                3L,
                FROM,
                TO,
                summary("843000", 88.0),
                null,
                chain(),
                Set.of(),
                List.of("GIAMHE10 - Giảm hè (toàn chuỗi, đến 2026-11-23)"),
                input("""
						{"summary": "Đang kín phòng, chưa cần giảm.", "campaigns": []}
						"""),
                notes);

        assertThat(advice.getCampaigns()).isEmpty();
        assertThat(advice.getRunningVouchers()).hasSize(1);
        assertThat(notes).containsExactly("Số liệu kỳ này chưa cho thấy chỗ nào cần giảm giá.");
    }
}
