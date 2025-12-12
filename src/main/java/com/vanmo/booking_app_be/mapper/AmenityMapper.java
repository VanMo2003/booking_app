package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.AmenityCreateRequest;
import com.vanmo.booking_app_be.dto.request.AmenityUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.entity.Amenity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AmenityMapper {

    @Mapping(target = "hotel", ignore = true)
    Amenity toEntity(AmenityCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "hotel", ignore = true)
    void updateEntity(@MappingTarget Amenity entity, AmenityUpdateRequest req);

    @Mapping(source = "hotel.id", target = "hotelId")
    @Mapping(target = "common", ignore = true)
//    @Mapping(source = "isCommon", target = "common")
    AmenityResponse toResponse(Amenity entity);
}
