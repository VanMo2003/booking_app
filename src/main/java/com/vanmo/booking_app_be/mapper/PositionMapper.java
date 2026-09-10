package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.PositionCreateRequest;
import com.vanmo.booking_app_be.dto.request.PositionUpdateRequest;
import com.vanmo.booking_app_be.dto.response.PositionResponse;
import com.vanmo.booking_app_be.entity.Position;

@Mapper(componentModel = "spring")
public interface PositionMapper {

    Position toEntity(PositionCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Position entity, PositionUpdateRequest req);

    PositionResponse toResponse(Position entity);
}
