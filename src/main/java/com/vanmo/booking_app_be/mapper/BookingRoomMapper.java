package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.BookingRoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.BookingRoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.BookingRoomResponse;
import com.vanmo.booking_app_be.entity.BookingRoom;

@Mapper(componentModel = "spring")
public interface BookingRoomMapper {

    BookingRoom toEntity(BookingRoomCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget BookingRoom entity, BookingRoomUpdateRequest req);

    BookingRoomResponse toResponse(BookingRoom entity);
}
