package com.vanmo.booking_app_be.dto.request;

import com.vanmo.booking_app_be.entity.Room;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeCreateRequest {
    String name; // Standard(tiêu chuẩn), Deluxe(hạng sang), Suite(cao cấp)
    String description;
}
