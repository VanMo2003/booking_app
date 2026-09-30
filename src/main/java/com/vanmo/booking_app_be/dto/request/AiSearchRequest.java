package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiSearchRequest {
    /** What the guest typed, e.g. "phòng 4 người ở Hà Đông cuối tuần này dưới 1 triệu". */
    @NotBlank
    @Size(max = 300)
    String query;
}
