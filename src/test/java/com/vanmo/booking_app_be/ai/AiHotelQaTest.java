package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiHotelAnswerResponse;
import com.vanmo.booking_app_be.entity.Tour;

/** The answer is the model's words, but what it may link to - and when it may claim to know - is decided here. */
class AiHotelQaTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static Tour tour(long id) {
        return Tour.builder().id(id).name("Food tour phố cổ").build();
    }

    @Test
    void chi_cho_phep_tro_toi_tour_cua_chinh_co_so() {
        Set<String> targets = AiHotelQa.allowedTargets(List.of(tour(7), tour(9)));

        // Chỉ tours/<mã> mới mở được trong app, nên không mở đường cho bất kỳ dạng liên kết nào khác.
        assertThat(targets).containsExactlyInAnyOrder("tours/7", "tours/9");
    }

    @Test
    void cau_hoi_nam_trong_the_question() {
        assertThat(AiHotelQa.prompt("Có đón sân bay không?"))
                .isEqualTo("<question>\nCó đón sân bay không?\n</question>");
    }

    @Test
    void giu_lien_ket_dung_tour_va_bo_lien_ket_la() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiHotelAnswerResponse answer = AiHotelQa.build(
                6L,
                "Có tour ăn uống buổi tối không?",
                input(
                        """
						{
						"answered": true,
						"answer": "Dạ cơ sở mình có Food tour phố cổ, 450.000 đ mỗi khách, khởi hành 18:00 hằng ngày.",
						"links": [
							{"label": "Xem tour Food tour phố cổ", "target": "bookingapp://tours/7"},
							{"label": "Xem lại tour đó", "target": "tours/7"},
							{"label": "Tour của cơ sở khác", "target": "tours/99"},
							{"label": "Trang cơ sở", "target": "/hotels/6"}
						]
						}
						"""),
                AiHotelQa.allowedTargets(List.of(tour(7))),
                notes);

        assertThat(answer.isAnswered()).isTrue();
        assertThat(answer.getHotelId()).isEqualTo(6L);
        // Tiền tố bookingapp:// được bỏ; bản trùng, tour của cơ sở khác và liên kết trang cơ sở đều bị loại.
        assertThat(answer.getLinks()).hasSize(1);
        assertThat(answer.getLinks().stream().map(AiHotelAnswerResponse.Link::getTarget))
                .containsExactly("tours/7");
        assertThat(notes).hasSize(1);
        assertThat(notes.getFirst()).contains("3 liên kết");
    }

    @Test
    void khong_tra_loi_duoc_thi_khong_kem_lien_ket_nao() throws JsonProcessingException {
        AiHotelAnswerResponse answer = AiHotelQa.build(
                6L,
                "Mấy giờ được nhận phòng?",
                input(
                        """
						{
						"answered": false,
						"answer": "Phần giờ nhận phòng cơ sở chưa cập nhật, anh/chị nhắn cho cơ sở giúp mình nhé.",
						"links": [{"label": "Trang cơ sở", "target": "hotels/6"}]
						}
						"""),
                AiHotelQa.allowedTargets(List.of()),
                new ArrayList<>());

        assertThat(answer.isAnswered()).isFalse();
        assertThat(answer.getAnswer()).contains("nhắn cho cơ sở");
        assertThat(answer.getLinks()).isEmpty();
    }

    @Test
    void noi_la_tra_loi_duoc_nhung_bo_trong_thi_van_tinh_la_khong_biet() throws JsonProcessingException {
        AiHotelAnswerResponse answer = AiHotelQa.build(
                6L,
                "Có bể bơi không?",
                input("""
						{"answered": true, "answer": "   "}
						"""),
                AiHotelQa.allowedTargets(List.of()),
                new ArrayList<>());

        assertThat(answer.isAnswered()).isFalse();
        assertThat(answer.getAnswer()).isNull();
    }
}
