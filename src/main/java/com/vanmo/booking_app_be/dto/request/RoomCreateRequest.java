package com.vanmo.booking_app_be.dto.request;

import static com.vanmo.booking_app_be.entity.enums.RoomStatus.AVAILABLE;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import com.vanmo.booking_app_be.entity.enums.RoomStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCreateRequest {
    @NotNull
    String roomNumber;

    @NotNull
    BigDecimal price;

    String description;

    @NotNull
    Integer capacity;

    @NotNull
    Long hotelId;

    @NotNull
    Long roomTypeId;

    @NotNull
    RoomStatus status = AVAILABLE;
}
