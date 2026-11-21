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
import com.vanmo.booking_app_be.dto.response.AiTranslationResponse;
import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.Dish;
import com.vanmo.booking_app_be.entity.Hotel;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.exception.AppException;

/** The model only fills in translations: the list of items, and which keys may appear, are decided here. */
class AiTranslatorTest {

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

    private static Amenity amenity(long id, String name) {
        Amenity amenity = new Amenity();
        amenity.setId(id);
        amenity.setName(name);
        return amenity;
    }

    private static com.vanmo.booking_app_be.entity.Service service(long id, String name, String description) {
        com.vanmo.booking_app_be.entity.Service service = new com.vanmo.booking_app_be.entity.Service();
        service.setId(id);
        service.setName(name);
        service.setDescription(description);
        service.setUnitPrice(new BigDecimal("150000"));
        return service;
    }

    private static Dish dish(long id, String name) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setName(name);
        return dish;
    }

    private static List<AiTranslationResponse.Item> sample() {
        return AiTranslator.items(
                hotel("Khách sạn nằm trong phố cổ, đi bộ ra Hồ Hoàn Kiếm 5 phút."),
                List.of(amenity(1, "Hồ bơi"), amenity(2, "   ")),
                List.of(service(3, "Đưa đón sân bay", null)),
                List.of(dish(4, "Bún chả")),
                List.of(Tour.builder()
                        .id(5L)
                        .name("Food tour phố cổ")
                        .description(null)
                        .build()));
    }

    @Test
    void chi_nhan_ngon_ngu_app_co_ho_tro() {
        assertThat(AiTranslator.language("EN")).isEqualTo("en");
        assertThat(AiTranslator.language(" ko ")).isEqualTo("ko");

        assertThatThrownBy(() -> AiTranslator.language("vi")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> AiTranslator.language(null)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> AiTranslator.language("")).isInstanceOf(AppException.class);
    }

    @Test
    void bo_muc_rong_va_gan_key_theo_tung_dong_that() {
        List<AiTranslationResponse.Item> items = sample();

        // Tiện ích chỉ có khoảng trắng và mô tả null đều không thành mục nào để dịch.
        assertThat(items.stream().map(AiTranslationResponse.Item::getKey))
                .containsExactly("hotel_description", "amenity_1", "service_3", "dish_4", "tour_5");
        assertThat(items.getFirst().getSection()).isEqualTo("DESCRIPTION");
        assertThat(items.get(2).getSection()).isEqualTo("SERVICE");
    }

    @Test
    void cat_mot_muc_qua_dai_thay_vi_gui_ca_quyen() {
        String long_ = "a".repeat(AiTranslator.MAX_ITEM_CHARS + 50);
        List<AiTranslationResponse.Item> items =
                AiTranslator.items(hotel(long_), List.of(), List.of(), List.of(), List.of());

        assertThat(items.getFirst().getOriginal()).hasSize(AiTranslator.MAX_ITEM_CHARS);
    }

    @Test
    void khong_gui_qua_so_muc_toi_da() {
        List<Dish> many = new ArrayList<>();
        for (int id = 1; id <= AiTranslator.MAX_ITEMS + 10; id++) {
            many.add(dish(id, "Món " + id));
        }

        List<AiTranslationResponse.Item> items =
                AiTranslator.items(hotel("Mô tả cơ sở."), List.of(), List.of(), many, List.of());

        assertThat(items).hasSize(AiTranslator.MAX_ITEMS);
    }

    @Test
    void prompt_neu_ngon_ngu_dich_va_tung_key() {
        String prompt = AiTranslator.prompt("English", sample());

        assertThat(prompt).startsWith("Dịch sang English.");
        assertThat(prompt).contains("hotel_description: Khách sạn nằm trong phố cổ");
        assertThat(prompt).contains("dish_4: Bún chả");
    }

    @Test
    void chi_giu_ban_dich_cua_key_da_gui_va_bao_so_muc_con_thieu() throws JsonProcessingException {
        List<AiTranslationResponse.Item> items = sample();
        List<String> notes = new ArrayList<>();

        AiTranslationResponse response = AiTranslator.build(
                6L,
                "en",
                items,
                input(
                        """
						{
						"items": [
							{"key": "hotel_description", "text": "In the Old Quarter, a 5-minute walk to Hoan Kiem Lake."},
							{"key": "amenity_1", "text": "Swimming pool"},
							{"key": "amenity_1", "text": "Pool (duplicate)"},
							{"key": "dish_99", "text": "A dish that does not exist"},
							{"key": "dish_4", "text": "Bun cha (grilled pork with noodles)"}
						]
						}
						"""),
                notes);

        assertThat(response.getLang()).isEqualTo("en");
        assertThat(response.getLanguage()).isEqualTo("English");
        // Danh sách vẫn đúng 5 mục của cơ sở: món không có thật không thêm được dòng nào.
        assertThat(response.getItems()).hasSize(5);
        assertThat(response.getItems().get(1).getTranslated()).isEqualTo("Swimming pool");
        assertThat(response.getItems().get(3).getTranslated()).isEqualTo("Bun cha (grilled pork with noodles)");
        // Hai mục model bỏ qua giữ bản gốc, không bị bịa.
        assertThat(response.getItems().get(2).getTranslated()).isNull();
        assertThat(response.getItems().get(2).getOriginal()).isEqualTo("Đưa đón sân bay");
        assertThat(notes).hasSize(2);
        assertThat(notes.getFirst()).contains("2 mục chưa dịch được");
        assertThat(notes.get(1)).contains("2 mục không thuộc trang");
    }
}
