package com.vanmo.booking_app_be.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** The branches a guest wants compared - two to four of them. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiHotelCompareRequest {
    @NotEmpty
    @Size(min = 2, max = 4)
    List<Long> hotelIds;
}
