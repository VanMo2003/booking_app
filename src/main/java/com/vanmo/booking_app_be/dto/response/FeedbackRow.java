package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One guest review with its branch, for the rating report. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackRow {
    Long feedbackId;
    Long bookingId;
    Long hotelId;
    String hotelName;
    String customerName;
    Integer rating;
    String comments;
    LocalDate reviewDate;
}
