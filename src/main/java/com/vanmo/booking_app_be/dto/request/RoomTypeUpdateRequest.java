package com.vanmo.booking_app_be.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeUpdateRequest {
    String name; // Standard, Deluxe, Suite
    String description;
}
