package com.vanmo.booking_app_be.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.vanmo.booking_app_be.entity.enums.DishCategory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DishCreateRequest {
    @NotBlank
    @Size(max = 255)
    String name;

    String description;

    @NotNull
    @Positive
    BigDecimal price;

    @NotNull
    DishCategory category;

    @Size(max = 1000)
    String pathImage;

    @Builder.Default
    boolean available = true;

    @NotNull
    Long hotelId;
}
