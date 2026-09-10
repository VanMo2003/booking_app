package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.HotelImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelImageResponse;
import com.vanmo.booking_app_be.entity.HotelImage;

@Mapper(componentModel = "spring")
public interface HotelImageMapper {

    HotelImage toEntity(HotelImageCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget HotelImage entity, HotelImageUpdateRequest req);

    HotelImageResponse toResponse(HotelImage entity);
}
