package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiHotelCompareResponse;
import com.vanmo.booking_app_be.exception.AppException;

/** The table is counted in code, so the ids accepted and the lines kept are what the test pins down. */
class AiHotelCompareTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static AiHotelCompareResponse.Item item(long id, String name, String from, String to, int capacity) {
        return AiHotelCompareResponse.Item.builder()
                .hotelId(id)
                .name(name)
                .category("Standard")
                .address("12 Hàng Bạc, Hà Nội")
                .priceFrom(from == null ? null : new BigDecimal(from))
                .priceTo(to == null ? null : new BigDecimal(to))
                .maxCapacity(capacity)
                .rooms(8)
                .amenities(5)
                .services(3)
                .tours(2)
                .reviews(12)
                .averageRating(4.3)
                .build();
    }

    @Test
    void bo_ma_trung_va_doi_hoi_it_nhat_hai_co_so() {
        assertThat(AiHotelCompare.ids(List.of(6L, 7L, 6L))).containsExactly(6L, 7L);
        // Thứ tự khách chọn được giữ nguyên để bảng không nhảy chỗ.
        assertThat(AiHotelCompare.ids(List.of(9L, 6L))).containsExactly(9L, 6L);

        assertThatThrownBy(() -> AiHotelCompare.ids(List.of(6L, 6L))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> AiHotelCompare.ids(List.of(6L))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> AiHotelCompare.ids(null)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> AiHotelCompare.ids(Arrays.asList(6L, null))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> AiHotelCompare.ids(List.of(1L, 2L, 3L, 4L, 5L)))
                .isInstanceOf(AppException.class);
    }

    @Test
    void bang_mang_theo_ma_gia_so_khach_va_so_danh_gia() {
        String prompt = AiHotelCompare.prompt(List.of(
                item(6, "Hoàn Kiếm Palace", "800000", "2500000", 4), item(7, "Ba Đình Riverside", null, null, 2)));

        assertThat(prompt).contains("[mã 6] Hoàn Kiếm Palace").contains("[mã 7] Ba Đình Riverside");
        assertThat(prompt).contains("phòng lớn nhất 4 khách").contains("12 đánh giá, trung bình 4.3/5");
        // Cơ sở chưa có phòng nào có giá thì nói rõ, chứ không hiện 0 đ.
        assertThat(prompt).contains("(chưa có phòng nào có giá)");
    }

    @Test
    void co_so_chua_co_danh_gia_thi_khong_hien_diem_trung_binh() {
        AiHotelCompareResponse.Item fresh = item(7, "Ba Đình Riverside", "900000", "1200000", 2);
        fresh.setReviews(0);
        fresh.setAverageRating(0);

        assertThat(AiHotelCompare.prompt(List.of(fresh))).contains("chưa có đánh giá");
    }

    @Test
    void gan_nhan_xet_vao_dung_co_so_va_bo_nhan_xet_la() throws JsonProcessingException {
        List<AiHotelCompareResponse.Item> items = new ArrayList<>(List.of(
                item(6, "Hoàn Kiếm Palace", "800000", "2500000", 4),
                item(7, "Ba Đình Riverside", "900000", "1200000", 2)));
        List<String> notes = new ArrayList<>();

        AiHotelCompareResponse compared = AiHotelCompare.build(
                items,
                input(
                        """
						{
						"verdict": "Đi cùng gia đình thì Hoàn Kiếm Palace, đi công tác một mình thì Ba Đình Riverside.",
						"differences": [
							"Hoàn Kiếm Palace có phòng 4 khách, Ba Đình Riverside tối đa 2 khách.",
							"Hoàn Kiếm Palace có phòng 4 khách, Ba Đình Riverside tối đa 2 khách.",
							"Giá mở đầu của Hoàn Kiếm Palace thấp hơn 100.000 đ.",
							"Cả hai đều có tour của cơ sở.",
							"Hoàn Kiếm Palace nhiều dịch vụ tính thêm hơn.",
							"Dòng thứ năm bị cắt."
						],
						"fits": [
							{"hotel_id": 6, "fit_for": "Phù hợp gia đình có trẻ nhỏ."},
							{"hotel_id": 6, "fit_for": "Nhận xét thứ hai cho cùng cơ sở."},
							{"hotel_id": 7, "fit_for": "Phù hợp khách đi công tác."},
							{"hotel_id": 99, "fit_for": "Cơ sở không ai hỏi tới."}
						]
						}
						"""),
                notes);

        assertThat(compared.getItems().get(0).getFitFor()).isEqualTo("Phù hợp gia đình có trẻ nhỏ.");
        assertThat(compared.getItems().get(1).getFitFor()).isEqualTo("Phù hợp khách đi công tác.");
        assertThat(compared.getVerdict()).startsWith("Đi cùng gia đình");
        // Dòng trùng bị bỏ và chỉ giữ 4 khác biệt để bảng không thành một bài dài.
        assertThat(compared.getDifferences()).hasSize(4);
        assertThat(compared.getDifferences()).doesNotContain("Dòng thứ năm bị cắt.");
        assertThat(notes).hasSize(1);
        assertThat(notes.getFirst()).contains("2 nhận xét");
    }

    @Test
    void co_so_khong_duoc_nhan_xet_thi_de_trong_chu_khong_bia() throws JsonProcessingException {
        List<AiHotelCompareResponse.Item> items =
                new ArrayList<>(List.of(item(6, "Hoàn Kiếm Palace", "800000", "2500000", 4)));

        AiHotelCompareResponse compared = AiHotelCompare.build(
                items,
                input("""
						{"verdict": "Chỉ có một lựa chọn.", "differences": [], "fits": []}
						"""),
                new ArrayList<>());

        assertThat(compared.getItems().getFirst().getFitFor()).isNull();
        assertThat(compared.getDifferences()).isEmpty();
    }
}
