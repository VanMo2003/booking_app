package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.BookingCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingResponse;
import com.vanmo.booking_app_be.dto.response.ListBookingResponse;
import com.vanmo.booking_app_be.entity.Booking;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "handledBy", ignore = true)
    @Mapping(target = "bookingRooms", ignore = true)
    @Mapping(target = "bookingServices", ignore = true)
    Booking toEntity(BookingCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "handledBy", ignore = true)
    @Mapping(target = "bookingRooms", ignore = true)
    @Mapping(target = "bookingServices", ignore = true)
    void updateEntity(@MappingTarget Booking entity, BookingUpdateRequest req);

    @Mapping(source = "handledBy.id", target = "handledByAccountId")
    BookingResponse toResponse(Booking entity);

    ListBookingResponse toListResponse(Booking entity);
}
