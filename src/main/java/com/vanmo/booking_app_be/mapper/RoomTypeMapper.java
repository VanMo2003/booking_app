package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.RoomTypeCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomTypeUpdateRequest;
import com.vanmo.booking_app_be.dto.response.RoomTypeResponse;
import com.vanmo.booking_app_be.entity.RoomType;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomTypeMapper {

    RoomType toEntity(RoomTypeCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget RoomType entity, RoomTypeUpdateRequest req);

    RoomTypeResponse toResponse(RoomType entity);
}
