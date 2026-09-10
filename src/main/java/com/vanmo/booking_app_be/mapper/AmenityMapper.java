package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.AmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.AmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.entity.Amenity;

@Mapper(componentModel = "spring")
public interface AmenityMapper {

    @Mapping(target = "hotel", ignore = true)
    //    @Mapping(target = "room", ignore = true)
    Amenity toEntity(AmenityCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "hotel", ignore = true)
    //    @Mapping(target = "room", ignore = true)
    void updateEntity(@MappingTarget Amenity entity, AmenityUpdateRequest req);

    @Mapping(target = "hotelName", ignore = true)
    @Mapping(target = "roomName", ignore = true)
    AmenityResponse toResponse(Amenity entity);
}
