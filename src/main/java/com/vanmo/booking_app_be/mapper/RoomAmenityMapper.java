package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.RoomAmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomAmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.RoomAmenityResponse;
import com.vanmo.booking_app_be.entity.RoomAmenity;

@Mapper(componentModel = "spring")
public interface RoomAmenityMapper {

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "amenity", ignore = true)
    RoomAmenity toEntity(RoomAmenityCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget RoomAmenity entity, RoomAmenityUpdateRequest req);

    RoomAmenityResponse toResponse(RoomAmenity entity);
}
