package com.vanmo.booking_app_be.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanmo.booking_app_be.dto.response.AiDescriptionResponse;
import com.vanmo.booking_app_be.entity.Dish;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Room;
import com.vanmo.booking_app_be.entity.RoomType;
import com.vanmo.booking_app_be.entity.enums.DishCategory;
import com.vanmo.booking_app_be.exception.AppException;

/** What the model is told about a row, and what survives of what it writes back. */
class AiDescriptionWriterTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static Hotel hotel() {
        return Hotel.builder()
                .id(6L)
                .name("Hoàn Kiếm Palace")
                .address("12 Hàng Bạc, Hà Nội")
                .build();
    }

    private static Room room() {
        RoomType type = new RoomType();
        type.setName("Deluxe");
        Room room = new Room();
        room.setId(11L);
        room.setRoomNumber("301");
        room.setRoomType(type);
        room.setCapacity(3);
        room.setPrice(new BigDecimal("1500000"));
        return room;
    }

    @Test
    void chi_nhan_ba_loai_muc() {
        assertThat(AiDescriptionWriter.kind("room")).isEqualTo(AiDescriptionWriter.Kind.ROOM);
        assertThat(AiDescriptionWriter.kind(" DISH ")).isEqualTo(AiDescriptionWriter.Kind.DISH);

        assertThatThrownBy(() -> AiDescriptionWriter.kind("TOUR")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> AiDescriptionWriter.kind(null)).isInstanceOf(AppException.class);
    }

    @Test
    void thong_tin_phong_khong_bao_gio_kem_gia() {
        String facts = AiDescriptionWriter.roomFacts(room(), List.of("Điều hoà", "Ban công"));

        assertThat(facts).contains("Hạng phòng: Deluxe").contains("Sức chứa: 3 khách");
        assertThat(facts).contains("Tiện nghi trong phòng: Điều hoà, Ban công");
        // Giá không nằm trong prompt, nên model không có gì để viết về giá.
        assertThat(facts).doesNotContain("1.500.000").doesNotContain("1500000");
    }

    @Test
    void phong_chua_khai_tien_nghi_thi_noi_ro_la_chua_co() {
        assertThat(AiDescriptionWriter.roomFacts(room(), List.of())).contains("(chưa khai tiện nghi nào)");
    }

    @Test
    void thong_tin_mon_an_mang_theo_nhom_mon_va_tinh_trang() {
        Dish dish = new Dish();
        dish.setId(4L);
        dish.setName("Bún chả");
        dish.setCategory(DishCategory.MAIN_COURSE);
        dish.setAvailable(false);

        String facts = AiDescriptionWriter.dishFacts(dish);

        assertThat(facts).contains("Tên món: Bún chả").contains("MAIN_COURSE").contains("tạm hết");
    }

    @Test
    void ghi_chu_quan_ly_vao_the_notes_va_bi_cat_khi_qua_dai() {
        String longNote = "x".repeat(AiDescriptionWriter.MAX_NOTES_CHARS + 100);

        String prompt = AiDescriptionWriter.prompt(hotel(), "Loại: phòng\n", longNote);

        assertThat(prompt).contains("Hoàn Kiếm Palace - 12 Hàng Bạc, Hà Nội");
        assertThat(prompt).contains("<notes>").contains("</notes>");
        assertThat(prompt).contains("x".repeat(AiDescriptionWriter.MAX_NOTES_CHARS));
        assertThat(prompt).doesNotContain("x".repeat(AiDescriptionWriter.MAX_NOTES_CHARS + 1));
    }

    @Test
    void khong_co_ghi_chu_thi_khong_co_the_notes_rong() {
        assertThat(AiDescriptionWriter.prompt(hotel(), "Loại: phòng\n", "   ")).doesNotContain("<notes>");
        assertThat(AiDescriptionWriter.prompt(hotel(), "Loại: phòng\n", null)).doesNotContain("<notes>");
    }

    @Test
    void giu_nhap_va_toi_da_bon_y_ngan() throws JsonProcessingException {
        AiDescriptionResponse drafted = AiDescriptionWriter.build(
                AiDescriptionWriter.Kind.ROOM,
                11L,
                null,
                input(
                        """
						{
						"description": "Phòng Deluxe 301 ngủ được 3 khách, có điều hoà và ban công riêng.",
						"highlights": ["3 khách", "Ban công riêng", "3 khách", "Điều hoà", "Gần hồ", "Thêm ý thừa"]
						}
						"""),
                new ArrayList<>());

        assertThat(drafted.getKind()).isEqualTo("ROOM");
        assertThat(drafted.getId()).isEqualTo(11L);
        // Ý trùng bị bỏ, và chỉ giữ 4 ý đầu.
        assertThat(drafted.getHighlights()).containsExactly("3 khách", "Ban công riêng", "Điều hoà", "Gần hồ");
        assertThat(drafted.getCurrentDescription()).isNull();
        assertThat(drafted.getNotes()).isEmpty();
    }

    @Test
    void da_co_mo_ta_thi_mang_theo_va_nhac_se_thay_ban_cu() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        AiDescriptionResponse drafted = AiDescriptionWriter.build(
                AiDescriptionWriter.Kind.DISH,
                4L,
                "  Món cũ viết vội.  ",
                input("""
						{"description": "Bún chả nướng than hoa, ăn kèm bún và rau sống."}
						"""),
                notes);

        assertThat(drafted.getCurrentDescription()).isEqualTo("Món cũ viết vội.");
        assertThat(drafted.getHighlights()).isEmpty();
        assertThat(notes).hasSize(1);
        assertThat(notes.getFirst()).contains("thay bản cũ");
    }

    @Test
    void khong_co_mo_ta_thi_coi_nhu_goi_model_that_bai() throws JsonProcessingException {
        assertThatThrownBy(() -> AiDescriptionWriter.build(
                        AiDescriptionWriter.Kind.SERVICE,
                        3L,
                        null,
                        input("""
						{"highlights": ["Nhanh"]}
						"""),
                        new ArrayList<>()))
                .isInstanceOf(AppException.class);
    }
}
