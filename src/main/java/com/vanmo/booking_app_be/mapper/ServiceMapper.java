package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.ServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.ServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.ServiceInfoResponse;
import com.vanmo.booking_app_be.dto.response.ServiceResponse;
import com.vanmo.booking_app_be.entity.Service;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    Service toEntity(ServiceCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Service entity, ServiceUpdateRequest req);

    ServiceResponse toResponse(Service entity);

    ServiceInfoResponse toInfoResponse(Service entity);
}
