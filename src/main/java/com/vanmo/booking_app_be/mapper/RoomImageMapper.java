package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.RoomImageCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomImageUpdateRequest;
import com.vanmo.booking_app_be.dto.response.RoomImageResponse;
import com.vanmo.booking_app_be.entity.RoomImage;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomImageMapper {

     RoomImage toEntity(RoomImageCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
     void updateEntity(@MappingTarget RoomImage entity, RoomImageUpdateRequest req);

    RoomImageResponse toResponse(RoomImage entity);
}
