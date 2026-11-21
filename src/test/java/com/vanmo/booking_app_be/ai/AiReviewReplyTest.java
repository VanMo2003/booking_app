package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiReviewReplyResponse;
import com.vanmo.booking_app_be.entity.Feedback;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.exception.AppException;

/** The draft is the model's wording; what it is told, and what survives, is decided here. */
class AiReviewReplyTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static Hotel hotel() {
        return Hotel.builder().id(6L).name("Hoàn Kiếm Palace").build();
    }

    private static Feedback review(Integer rating, String comments, String reply) {
        return Feedback.builder()
                .id(31L)
                .rating(rating)
                .comments(comments)
                .reply(reply)
                .reviewDate(LocalDate.of(2026, 11, 14))
                .build();
    }

    @Test
    void duoi_ba_sao_doc_la_danh_gia_chua_tot() {
        assertThat(AiReviewReply.low(1)).isTrue();
        assertThat(AiReviewReply.low(3)).isTrue();
        assertThat(AiReviewReply.low(4)).isFalse();
        assertThat(AiReviewReply.low(null)).isFalse();
    }

    @Test
    void loi_khach_nam_trong_the_review() {
        String prompt = AiReviewReply.prompt(hotel(), review(2, "  Phòng ồn, nhận phòng chậm.  ", null));

        assertThat(prompt).contains("Hoàn Kiếm Palace").contains("2/5").contains("một đánh giá chưa tốt");
        assertThat(prompt).contains("<review>\nPhòng ồn, nhận phòng chậm.\n</review>");
    }

    @Test
    void khach_chi_cho_sao_thi_noi_ro_la_khong_co_nhan_xet() {
        String prompt = AiReviewReply.prompt(hotel(), review(5, "   ", null));

        assertThat(prompt).contains("một đánh giá tốt");
        assertThat(prompt).contains("Khách không viết nhận xét");
        // Không có thẻ <review> rỗng để model tưởng là khách đã kể gì đó.
        assertThat(prompt).doesNotContain("<review>");
    }

    @Test
    void giu_nhap_va_toi_da_ba_diem_khach_neu() throws JsonProcessingException {
        AiReviewReplyResponse drafted = AiReviewReply.build(
                review(2, "Phòng ồn.", null),
                input(
                        """
						{
						"draft": "Cảm ơn anh/chị đã nói ra. Cơ sở mình đã ghi nhận chuyện phòng ồn và sẽ kiểm tra lại.",
						"points": ["Phòng ồn", "Phòng ồn", "Nhận phòng chậm", "Thang máy đông", "Bữa sáng ít món"]
						}
						"""),
                new ArrayList<>());

        assertThat(drafted.getFeedbackId()).isEqualTo(31L);
        assertThat(drafted.getRating()).isEqualTo(2);
        assertThat(drafted.getDraft()).startsWith("Cảm ơn anh/chị");
        // Điểm trùng bị bỏ, và chỉ giữ 3 điểm đầu để bảng không thành một danh sách dài.
        assertThat(drafted.getPoints()).containsExactly("Phòng ồn", "Nhận phòng chậm", "Thang máy đông");
        assertThat(drafted.getCurrentReply()).isNull();
        assertThat(drafted.getNotes()).isEmpty();
    }

    @Test
    void da_co_cau_tra_loi_thi_mang_theo_va_nhac_se_thay_cau_cu() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiReviewReplyResponse drafted = AiReviewReply.build(
                review(2, "Phòng ồn.", "  Cảm ơn chị, bên mình đã kê lại phòng.  "),
                input("""
						{"draft": "Cảm ơn anh/chị, cơ sở mình sẽ kiểm tra lại."}
						"""),
                notes);

        assertThat(drafted.getCurrentReply()).isEqualTo("Cảm ơn chị, bên mình đã kê lại phòng.");
        assertThat(drafted.getPoints()).isEmpty();
        assertThat(notes).hasSize(1);
        assertThat(notes.getFirst()).contains("thay câu cũ");
    }

    @Test
    void khong_co_nhap_thi_coi_nhu_goi_model_that_bai() throws JsonProcessingException {
        assertThatThrownBy(() -> AiReviewReply.build(
                        review(5, "Tốt.", null), input("""
						{"points": ["Tốt"]}
						"""), new ArrayList<>()))
                .isInstanceOf(AppException.class);
    }
}
