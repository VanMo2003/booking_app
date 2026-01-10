package com.vanmo.booking_app_be.dto.response;

import com.vanmo.booking_app_be.entity.enums.RoomStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomDetailResponse {
    Long id;
    String roomNumber;
    BigDecimal price;
    String description;
    Integer capacity;
    RoomStatus status;
    Long hotelId;
    String hotelName;
    Long roomTypeId;
    String roomTypeName;
    List<AmenityResponse> amenities;
    Date onCreate;
    Date onUpdate;
}
