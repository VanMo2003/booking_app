package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * A trip the assistant put together from real rows: where to sleep, which tours on which day, and what it costs.
 * The hotel, the room, the tours and every figure come from the database - the model only arranges them and writes
 * the day titles, so nothing here is a price or a tour it made up.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiTripPlanResponse {

    String query;

    /** One line the guest reads first, e.g. "Hạ Long 3 ngày 2 đêm cho 2 người lớn và 1 trẻ em". */
    String summary;

    String destination;

    LocalDate startDate;

    /** Nights at the hotel; the trip runs {@code nights + 1} days. */
    int nights;

    int adults;

    int children;

    Set<TravelTheme> themes;

    /** What the guest said they could spend on the whole trip, when they said it. */
    BigDecimal budget;

    Stay stay;

    List<Day> days;

    /** Every tour the plan uses, in the order the days need them - the app shows them with its usual cards. */
    List<TourResponse> tours;

    /** The best voucher the plan could use on the tour part, if any. */
    VoucherResponse voucher;

    Cost cost;

    /** Anything the guest should know: over budget, no room free, a day left open… */
    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Stay {
        Long hotelId;
        String hotelName;
        String address;
        String category;
        double rating;
        String pathImage;
        Long roomId;
        String roomNumber;
        String roomType;
        int capacity;
        BigDecimal pricePerNight;
        LocalDate checkin;
        LocalDate checkout;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Day {
        /** 1 for the first day of the trip. */
        int index;

        LocalDate date;

        String title;

        List<Step> steps;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step {
        /** "08:00", or null for something with no set time. */
        String time;

        String title;

        String detail;

        /** Set when the step is one of the plan's tours, so the app can open it. */
        Long tourId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Cost {
        /** Room price × nights. */
        BigDecimal rooms;

        /** Every tour in the plan, at today's price per adult and per child. */
        BigDecimal tours;

        BigDecimal voucherDiscount;

        BigDecimal total;

        BigDecimal perPerson;

        /** Budget − total; negative means over budget. */
        BigDecimal leftOver;
    }
}
