package com.vanmo.booking_app_be.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * An answer to a guest's question about their own trips, written from their own bookings, tour orders and vouchers.
 * {@link #links} are deep links the app can open, and only to things the guest actually has.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiGuestAnswerResponse {

    String question;

    String answer;

    /** What the guest could do next, one line each. */
    List<String> suggestions;

    List<Link> links;

    /** What the assistant could not see (the app has no such data), so the guest knows to ask the hotel. */
    List<String> notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Link {

        /** "Xem đơn phòng #26". */
        String label;

        /** A deep-link target the app understands, e.g. {@code bookings/26} or {@code tours/7}. */
        String target;
    }
}
