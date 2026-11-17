package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * One guest review, with the branch's answer when there is one. Before replies existed this carried only the id,
 * which is why the app could write a review but never show one.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedbackResponse {
    Long id;

    Long bookingId;

    Long hotelId;

    String hotelName;

    Long customerId;

    String customerName;

    Integer rating;

    String comments;

    LocalDate reviewDate;

    /** The branch's public answer, null when nobody has answered yet. */
    String reply;

    LocalDateTime replyAt;
}
