package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.DishCreateRequest;
import com.vanmo.booking_app_be.dto.request.DishUpdateRequest;
import com.vanmo.booking_app_be.dto.response.DishResponse;
import com.vanmo.booking_app_be.entity.Dish;

@Mapper(componentModel = "spring")
public interface DishMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "onCreate", ignore = true)
    @Mapping(target = "onUpdate", ignore = true)
    Dish toEntity(DishCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "onCreate", ignore = true)
    @Mapping(target = "onUpdate", ignore = true)
    void updateEntity(@MappingTarget Dish entity, DishUpdateRequest req);

    @Mapping(source = "hotel.id", target = "hotelId")
    DishResponse toResponse(Dish entity);
}
