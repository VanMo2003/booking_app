package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.vanmo.booking_app_be.entity.enums.DishCategory;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Partial update: fields left null keep their value. Send an empty pathImage to remove the photo. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DishUpdateRequest {
    @Size(min = 1, max = 255)
    String name;

    String description;

    @Positive
    BigDecimal price;

    DishCategory category;

    @Size(max = 1000)
    String pathImage;

    Boolean available;
}
