package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.RoomCreateRequest;
import com.vanmo.booking_app_be.dto.request.RoomUpdateRequest;
import com.vanmo.booking_app_be.dto.response.RoomDetailResponse;
import com.vanmo.booking_app_be.dto.response.RoomResponse;
import com.vanmo.booking_app_be.entity.Room;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "roomType", ignore = true)
    Room toEntity(RoomCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "roomType", ignore = true)
    void updateEntity(@MappingTarget Room room, RoomUpdateRequest req);

    @Mapping(source = "hotel.id", target = "hotelId")
    @Mapping(source = "hotel.name", target = "hotelName")
    @Mapping(source = "roomType.id", target = "roomTypeId")
    @Mapping(source = "roomType.name", target = "roomTypeName")
    RoomResponse toResponse(Room room);

    @Mapping(source = "hotel.id", target = "hotelId")
    @Mapping(source = "hotel.name", target = "hotelName")
    @Mapping(source = "roomType.id", target = "roomTypeId")
    @Mapping(source = "roomType.name", target = "roomTypeName")
    @Mapping(target = "amenities", ignore = true)
    RoomDetailResponse toDetailResponse(Room room);
}
