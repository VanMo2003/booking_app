package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.TourCreateRequest;
import com.vanmo.booking_app_be.dto.request.TourUpdateRequest;
import com.vanmo.booking_app_be.dto.response.TourResponse;
import com.vanmo.booking_app_be.entity.Tour;

@Mapper(componentModel = "spring")
public interface TourMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "onCreate", ignore = true)
    @Mapping(target = "onUpdate", ignore = true)
    Tour toEntity(TourCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "onCreate", ignore = true)
    @Mapping(target = "onUpdate", ignore = true)
    void updateEntity(@MappingTarget Tour entity, TourUpdateRequest req);

    @Mapping(source = "hotel.id", target = "hotelId")
    TourResponse toResponse(Tour entity);
}
