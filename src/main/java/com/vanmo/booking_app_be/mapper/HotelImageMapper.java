package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.HotelImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelImageResponse;
import com.vanmo.booking_app_be.entity.HotelImage;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface HotelImageMapper {

     HotelImage toEntity(HotelImageCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
     void updateEntity(@MappingTarget HotelImage entity, HotelImageUpdateRequest req);

    HotelImageResponse toResponse(HotelImage entity);
}
