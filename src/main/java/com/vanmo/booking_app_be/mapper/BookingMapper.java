package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.entity.Booking;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "bookingRooms", ignore = true)
    @Mapping(target = "bookingServices", ignore = true)
    Booking toEntity(BookingCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bookingRooms", ignore = true)
    @Mapping(target = "bookingServices", ignore = true)
    void updateEntity(@MappingTarget Booking entity, BookingUpdateRequest req);

    BookingResponse toResponse(Booking entity);
}
