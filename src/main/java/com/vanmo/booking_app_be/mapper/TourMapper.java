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
    @Mapping(target = "tourRooms", ignore = true)
    @Mapping(target = "stayNights", ignore = true)
    @Mapping(target = "themes", ignore = true)
    @Mapping(target = "itinerary", ignore = true)
    @Mapping(target = "featured", ignore = true)
    Tour toEntity(TourCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "onCreate", ignore = true)
    @Mapping(target = "onUpdate", ignore = true)
    @Mapping(target = "tourRooms", ignore = true)
    @Mapping(target = "themes", ignore = true)
    @Mapping(target = "itinerary", ignore = true)
    @Mapping(target = "featured", ignore = true)
    void updateEntity(@MappingTarget Tour entity, TourUpdateRequest req);

    @Mapping(source = "hotel.id", target = "hotelId")
    @Mapping(source = "hotel.name", target = "hotelName")
    @Mapping(source = "hotel.address", target = "hotelAddress")
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "itinerary", ignore = true)
    @Mapping(target = "onSale", ignore = true)
    @Mapping(target = "salePrice", ignore = true)
    @Mapping(target = "saleChildPrice", ignore = true)
    TourResponse toResponse(Tour entity);
}
