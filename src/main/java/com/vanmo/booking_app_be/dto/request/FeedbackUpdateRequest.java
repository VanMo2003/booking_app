package com.vanmo.booking_app_be.dto.request;

import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackUpdateRequest {
    int rating;
    String comments;
    LocalDate reviewDate = LocalDate.now();
}
