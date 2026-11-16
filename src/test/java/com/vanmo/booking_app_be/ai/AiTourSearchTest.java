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
import com.vanmo.booking_app_be.dto.response.AiTourSearchResponse.Filters;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.dto.response.TourRoomResponse;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

/**
 * The two pure halves of the natural-language tour search: model output → validated filters, and a tour's score
 * against them. No model and no database.
 */
class AiTourSearchTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    private static JsonNode input(String json) throws JsonProcessingException {
        return JSON.readTree(json);
    }

    private static TourResponse tour(BigDecimal price, Set<TravelTheme> themes) {
        return TourResponse.builder()
                .id(1L)
                .name("Du thuyền vịnh Hạ Long")
                .destination("Hạ Long, Quảng Ninh")
                .hotelAddress("12 Bãi Cháy, Hạ Long")
                .includes("Xe đưa đón, vé tham quan, ăn trưa trên tàu")
                .price(price)
                .salePrice(price)
                .themes(themes)
                .available(true)
                .build();
    }

    @Test
    void readsFiltersAndDropsWhatCannotBeUsed() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        Filters filters = AiTourSearch.filters(
                input(
                        """
				{
				"destinations": ["Hạ Long", "Quảng Ninh", ""],
				"themes": ["BEACH", "SPACE"],
				"min_price": 2000000,
				"max_price": 1000000,
				"adults": 2,
				"children": 1,
				"tour_date": "2026-11-20",
				"nights": 1,
				"with_rooms": true,
				"keywords": ["du thuyền"],
				"summary": "Tour biển Hạ Long 2 người lớn 1 trẻ em"
				}
				"""),
                TODAY,
                notes);

        assertThat(filters.getDestinations()).containsExactly("Hạ Long", "Quảng Ninh");
        // An unknown theme is ignored, not an error.
        assertThat(filters.getThemes()).containsExactly(TravelTheme.BEACH);
        // min > max is the model mixing them up: swap instead of returning nothing.
        assertThat(filters.getMinPrice()).isEqualByComparingTo("1000000");
        assertThat(filters.getMaxPrice()).isEqualByComparingTo("2000000");
        assertThat(filters.getAdults()).isEqualTo(2);
        assertThat(filters.getChildren()).isEqualTo(1);
        assertThat(filters.getTourDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(filters.getWithRooms()).isTrue();
        assertThat(notes).isEmpty();
    }

    @Test
    void dropsADayThatHasPassedWithANote() throws JsonProcessingException {
        List<String> notes = new ArrayList<>();

        Filters filters = AiTourSearch.filters(
                input("""
				{"tour_date": "2026-11-01", "summary": "Tour hôm qua"}
				"""), TODAY, notes);

        assertThat(filters.getTourDate()).isNull();
        assertThat(notes).hasSize(1).first().asString().contains("đã qua");
    }

    @Test
    void placeAndPriceAreHardConditions() {
        TourResponse halong = tour(new BigDecimal("1200000"), Set.of(TravelTheme.BEACH));

        assertThat(AiTourSearch.score(
                        halong, Filters.builder().destinations(List.of("Sa Pa")).build()))
                .isEqualTo(-1);
        // Accent- and case-insensitive, and the branch address counts as the place too.
        assertThat(AiTourSearch.score(
                        halong,
                        Filters.builder().destinations(List.of("ha long")).build()))
                .isGreaterThanOrEqualTo(0);
        assertThat(AiTourSearch.score(
                        halong,
                        Filters.builder().maxPrice(new BigDecimal("1000000")).build()))
                .isEqualTo(-1);
        assertThat(AiTourSearch.score(
                        halong,
                        Filters.builder().minPrice(new BigDecimal("2000000")).build()))
                .isEqualTo(-1);
    }

    @Test
    void themesAndKeywordsOnlyMoveATourUpTheList() {
        TourResponse beach = tour(new BigDecimal("1200000"), Set.of(TravelTheme.BEACH, TravelTheme.NATURE));
        TourResponse food = tour(new BigDecimal("1200000"), Set.of(TravelTheme.FOOD));

        Filters wantsBeach = Filters.builder().themes(Set.of(TravelTheme.BEACH)).build();

        // A tour with no matching theme is still offered, just lower down.
        assertThat(AiTourSearch.score(food, wantsBeach)).isZero();
        assertThat(AiTourSearch.score(beach, wantsBeach)).isGreaterThan(AiTourSearch.score(food, wantsBeach));

        Filters keywords =
                Filters.builder().keywords(List.of("du thuyền", "xe đưa đón")).build();
        assertThat(AiTourSearch.score(beach, keywords)).isEqualTo(6);
    }

    @Test
    void headCountRoomsAndNightsRuleToursOut() {
        TourResponse small = tour(new BigDecimal("500000"), Set.of());
        small.setMaxGuests(2);
        TourResponse pkg = tour(new BigDecimal("500000"), Set.of());
        pkg.setRooms(List.of(TourRoomResponse.builder().roomNumber("101").build()));
        pkg.setStayNights(2);

        assertThat(AiTourSearch.score(
                        small, Filters.builder().adults(2).children(1).build()))
                .isEqualTo(-1);
        assertThat(AiTourSearch.score(small, Filters.builder().adults(2).build()))
                .isGreaterThanOrEqualTo(0);
        // "Tour kèm khách sạn" and "đi về trong ngày" are opposites.
        assertThat(AiTourSearch.score(small, Filters.builder().withRooms(true).build()))
                .isEqualTo(-1);
        assertThat(AiTourSearch.score(pkg, Filters.builder().withRooms(false).build()))
                .isEqualTo(-1);
        // A package of 2 nights is too long for a guest who said one night.
        assertThat(AiTourSearch.score(pkg, Filters.builder().nights(1).build())).isEqualTo(-1);
        assertThat(AiTourSearch.score(pkg, Filters.builder().nights(2).build())).isGreaterThanOrEqualTo(0);
    }
}
