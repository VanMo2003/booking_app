package com.vanmo.booking_app_be.dto.request;

import com.vanmo.booking_app_be.entity.Amenity;
import com.vanmo.booking_app_be.entity.Room;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAmenityCreateRequest {
    Long roomId;
    Long amenityId;
    Integer quantity;
}
