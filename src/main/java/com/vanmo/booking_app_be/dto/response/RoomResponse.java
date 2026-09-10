package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.RoomStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomResponse {
    Long id;
    String pathImage;
    String roomNumber;
    BigDecimal price;
    String description;
    Integer capacity;
    RoomStatus status;
    Long hotelId;
    String hotelName;
    Long roomTypeId;
    String roomTypeName;
    Date onCreate;
    Date onUpdate;
}
