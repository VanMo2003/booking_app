package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.DishCategory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DishResponse {
    Long id;
    String name;
    String description;
    BigDecimal price;
    DishCategory category;
    String pathImage;
    boolean available;
    Long hotelId;
    Date onCreate;
    Date onUpdate;
}
