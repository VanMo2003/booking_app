package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiDailyBriefResponse;
import com.vanmo.booking_app_be.dto.response.FeedbackRow;
import com.vanmo.booking_app_be.dto.response.ReceivableDTO;
import com.vanmo.booking_app_be.dto.response.TargetProgressResponse;
import com.vanmo.booking_app_be.entity.Booking;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.TourBooking;
import com.vanmo.booking_app_be.entity.enums.RoomStatus;

/** Every figure in the brief is counted in code, so the counting and the levels are what the test pins down. */
class AiDailyBriefTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final Instant NOW =
            TODAY.atTime(8, 0).atZone(ChatAssistant.ZONE).toInstant();

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static Booking pending(Duration age) {
        return Booking.builder().id(1L).onCreate(Date.from(NOW.minus(age))).build();
    }

    private static TourBooking tour(LocalDate date, int guests) {
        return TourBooking.builder().tourDate(date).guests(guests).build();
    }

    private static FeedbackRow review(int rating, LocalDate date) {
        return FeedbackRow.builder().rating(rating).reviewDate(date).build();
    }

    private static ReceivableDTO receivable(String amount, LocalDate checkout) {
        ReceivableDTO row = new ReceivableDTO();
        row.setTotalAmount(new BigDecimal(amount));
        row.setCheckoutDate(checkout);
        return row;
    }

    private static Room room(RoomStatus status) {
        Room room = new Room();
        room.setStatus(status);
        return room;
    }

    /** A quiet desk: every signal present, every one of them QUIET. */
    private static List<AiDailyBriefResponse.Signal> quietDesk() {
        return AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                List.of(),
                List.of(),
                List.of(room(RoomStatus.AVAILABLE)),
                null);
    }

    private static AiDailyBriefResponse.Signal find(List<AiDailyBriefResponse.Signal> signals, String key) {
        return signals.stream()
                .filter(signal -> key.equals(signal.getKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("không có dòng " + key));
    }

    @Test
    void khong_co_viec_gi_thi_moi_dong_deu_quiet() {
        List<AiDailyBriefResponse.Signal> signals = quietDesk();

        assertThat(signals).hasSize(10);
        assertThat(signals).allMatch(signal -> AiDailyBrief.QUIET.equals(signal.getLevel()));
        assertThat(find(signals, "revenue_target").getValue()).isEqualTo("chưa đặt");
    }

    @Test
    void don_cho_qua_24_gio_la_urgent_con_don_vua_dat_chi_la_watch() {
        List<AiDailyBriefResponse.Signal> fresh = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(pending(Duration.ofHours(2))),
                List.of(),
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                null);
        assertThat(find(fresh, "pending_bookings").getLevel()).isEqualTo(AiDailyBrief.WATCH);

        List<AiDailyBriefResponse.Signal> stale = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(pending(Duration.ofHours(2)), pending(Duration.ofHours(30))),
                List.of(),
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                null);
        AiDailyBriefResponse.Signal signal = find(stale, "pending_bookings");
        assertThat(signal.getLevel()).isEqualTo(AiDailyBrief.URGENT);
        assertThat(signal.getValue()).isEqualTo("2 đơn");
        assertThat(signal.getDetail()).contains("1 đơn đã chờ quá 24 giờ");
    }

    @Test
    void tour_hom_nay_tach_khoi_tour_ngay_mai_va_cong_so_khach() {
        List<AiDailyBriefResponse.Signal> signals = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(),
                List.of(tour(TODAY, 4), tour(TODAY, 2), tour(TODAY.plusDays(1), 3)),
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                null);

        AiDailyBriefResponse.Signal today = find(signals, "tours_today");
        assertThat(today.getValue()).isEqualTo("2 đơn");
        assertThat(today.getDetail()).startsWith("6 khách");
        assertThat(today.getLevel()).isEqualTo(AiDailyBrief.URGENT);

        AiDailyBriefResponse.Signal tomorrow = find(signals, "tours_tomorrow");
        assertThat(tomorrow.getValue()).isEqualTo("1 đơn");
        assertThat(tomorrow.getDetail()).startsWith("3 khách");
        assertThat(tomorrow.getLevel()).isEqualTo(AiDailyBrief.WATCH);
    }

    @Test
    void chi_dem_danh_gia_thap_va_moi() {
        List<AiDailyBriefResponse.Signal> signals = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                List.of(
                        review(1, TODAY.minusDays(1)), // thấp và mới
                        review(2, TODAY.minusDays(7)), // vừa đúng mốc 7 ngày
                        review(2, TODAY.minusDays(8)), // thấp nhưng đã cũ
                        review(5, TODAY)), // mới nhưng không phải phàn nàn
                List.of(),
                List.of(),
                null);

        AiDailyBriefResponse.Signal signal = find(signals, "low_reviews");
        assertThat(signal.getValue()).isEqualTo("2 đánh giá");
        assertThat(signal.getLevel()).isEqualTo(AiDailyBrief.URGENT);
    }

    @Test
    void tien_chua_thu_qua_mot_tuan_moi_la_urgent() {
        List<AiDailyBriefResponse.Signal> recent = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                List.of(),
                List.of(receivable("1200000", TODAY.minusDays(2))),
                List.of(),
                null);
        AiDailyBriefResponse.Signal signal = find(recent, "receivables");
        assertThat(signal.getLevel()).isEqualTo(AiDailyBrief.WATCH);
        assertThat(signal.getDetail()).contains("2 ngày");

        List<AiDailyBriefResponse.Signal> overdue = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                List.of(),
                List.of(receivable("1200000", TODAY.minusDays(2)), receivable("800000", TODAY.minusDays(9))),
                List.of(),
                null);
        assertThat(find(overdue, "receivables").getLevel()).isEqualTo(AiDailyBrief.URGENT);
        assertThat(find(overdue, "receivables").getValue()).isEqualTo("2 đơn");
    }

    @Test
    void tro_ly_chuyen_cho_nguoi_that_thi_gap_hon_la_tin_chua_doc() {
        List<AiDailyBriefResponse.Signal> unreadOnly = AiDailyBrief.signals(
                NOW, TODAY, List.of(), List.of(), List.of(), List.of(), 0, 3, List.of(), List.of(), List.of(), null);
        assertThat(find(unreadOnly, "chat_waiting").getLevel()).isEqualTo(AiDailyBrief.WATCH);

        List<AiDailyBriefResponse.Signal> handoff = AiDailyBrief.signals(
                NOW, TODAY, List.of(), List.of(), List.of(), List.of(), 1, 0, List.of(), List.of(), List.of(), null);
        assertThat(find(handoff, "chat_waiting").getLevel()).isEqualTo(AiDailyBrief.URGENT);
    }

    @Test
    void chi_tieu_do_theo_lich_chu_khong_doi_chieu_voi_100() {
        // Ngày 16/30: đã qua 53% số ngày. Đạt 55% là đang đi đúng nhịp.
        TargetProgressResponse onTrack = TargetProgressResponse.builder()
                .target(new BigDecimal("100000000"))
                .achievedRate(55.0)
                .daysElapsed(16)
                .daysInMonth(30)
                .build();
        assertThat(AiDailyBrief.targetSignal(onTrack).getLevel()).isEqualTo(AiDailyBrief.QUIET);

        // Cũng ngày đó mà mới đạt 30% thì hụt 23 điểm - quá mốc cảnh báo.
        TargetProgressResponse behind = TargetProgressResponse.builder()
                .target(new BigDecimal("100000000"))
                .achievedRate(30.0)
                .projectedRate(62.0)
                .daysElapsed(16)
                .daysInMonth(30)
                .build();
        AiDailyBriefResponse.Signal signal = AiDailyBrief.targetSignal(behind);
        assertThat(signal.getLevel()).isEqualTo(AiDailyBrief.URGENT);
        assertThat(signal.getValue()).isEqualTo("30% chỉ tiêu");
        assertThat(signal.getDetail()).contains("53%").contains("62%");
    }

    @Test
    void phong_bao_tri_hien_ca_tong_so_phong() {
        List<AiDailyBriefResponse.Signal> signals = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                List.of(),
                List.of(),
                List.of(room(RoomStatus.AVAILABLE), room(RoomStatus.MAINTENANCE), room(RoomStatus.OCCUPIED)),
                null);

        AiDailyBriefResponse.Signal signal = find(signals, "rooms_maintenance");
        assertThat(signal.getValue()).isEqualTo("1/3 phòng");
        assertThat(signal.getLevel()).isEqualTo(AiDailyBrief.WATCH);
    }

    @Test
    void loi_nhac_mang_theo_key_va_muc_do_cua_tung_dong() {
        List<AiDailyBriefResponse.Signal> signals = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(pending(Duration.ofHours(30))),
                List.of(),
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                null);

        String prompt =
                AiDailyBrief.prompt(Hotel.builder().name("Hoàn Kiếm Palace").build(), TODAY, signals);

        assertThat(prompt).contains("Hoàn Kiếm Palace").contains("2026-11-16");
        assertThat(prompt).contains("[URGENT] pending_bookings: Đơn phòng chờ xác nhận = 1 đơn");
        assertThat(prompt).contains("[QUIET] arrivals_today");
    }

    @Test
    void bo_viec_tro_ly_bia_ra_tu_dong_khong_can_xu_ly() throws JsonProcessingException {
        List<AiDailyBriefResponse.Signal> signals = AiDailyBrief.signals(
                NOW,
                TODAY,
                List.of(),
                List.of(),
                List.of(pending(Duration.ofHours(30))),
                List.of(),
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                null);
        List<String> notes = new ArrayList<>();

        AiDailyBriefResponse brief = AiDailyBrief.build(
                7L,
                TODAY,
                signals,
                input(
                        """
						{
						"headline": "Hôm nay chủ yếu là dọn nốt mấy đơn còn treo.",
						"tasks": [
							{"title": "Xác nhận đơn đang treo", "priority": "HIGH", "why": "Khách chờ lâu sẽ huỷ.",
							"how": "Mở danh sách đơn chờ và gọi cho khách.", "signal_key": "pending_bookings"},
							{"title": "Xác nhận đơn đang treo", "priority": "HIGH", "how": "Lặp lại việc trên.",
							"signal_key": "pending_bookings"},
							{"title": "Gọi khách trả phòng", "priority": "MEDIUM", "how": "Nhắc khách.",
							"signal_key": "departures_today"},
							{"title": "Tăng giá cuối tuần", "priority": "HIGH", "how": "Đổi bảng giá.",
							"signal_key": "weekend_pricing"}
						]
						}
						"""),
                notes);

        assertThat(brief.getHeadline()).startsWith("Hôm nay chủ yếu");
        assertThat(brief.getDate()).isEqualTo(TODAY);
        assertThat(brief.getSignals()).hasSize(10);
        // Chỉ còn việc gắn với dòng thật sự đang cần xử lý: bản lặp, dòng QUIET và key bịa đều bị bỏ.
        assertThat(brief.getTasks()).hasSize(1);
        assertThat(brief.getTasks().getFirst().getSignalKey()).isEqualTo("pending_bookings");
        assertThat(brief.getTasks().getFirst().getPriority()).isEqualTo("HIGH");
        assertThat(notes).hasSize(1);
        assertThat(notes.getFirst()).contains("3 việc");
    }

    @Test
    void desk_yen_tinh_thi_moi_viec_tro_ly_de_xuat_deu_bi_bo() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiDailyBriefResponse brief = AiDailyBrief.build(
                7L,
                TODAY,
                quietDesk(),
                input(
                        """
						{
						"headline": "Hôm nay nhẹ việc.",
						"tasks": [{"title": "Kiểm tra phòng", "priority": "LOW", "how": "Đi một vòng.",
						"signal_key": "rooms_maintenance"}]
						}
						"""),
                notes);

        assertThat(brief.getTasks()).isEmpty();
        assertThat(brief.getHeadline()).isEqualTo("Hôm nay nhẹ việc.");
    }
}
