package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.BookingServiceCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingServiceUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingServiceResponse;
import com.vanmo.booking_app_be.entity.BookingService;

@Mapper(componentModel = "spring")
public interface BookingServiceMapper {

    BookingService toEntity(BookingServiceCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget BookingService entity, BookingServiceUpdateRequest req);

    BookingServiceResponse toResponse(BookingService entity);
}
