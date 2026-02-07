package com.vanmo.booking_app_be.dto.request;

import com.vanmo.booking_app_be.entity.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import static com.vanmo.booking_app_be.entity.enums.RoomStatus.AVAILABLE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCreateRequest {
    @NotNull String roomNumber;
    @NotNull BigDecimal price;
    String description;
    @NotNull Integer capacity;
    @NotNull Long hotelId;
    @NotNull Long roomTypeId;
    @NotNull RoomStatus status = AVAILABLE;
}
