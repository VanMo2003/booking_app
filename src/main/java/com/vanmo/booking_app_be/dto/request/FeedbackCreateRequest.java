package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackCreateRequest {
    Long bookingId;
    Long customerId;
    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    int rating;
    String comments;
    LocalDate reviewDate = LocalDate.now();
}
