package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelDetailResponse;
import com.vanmo.booking_app_be.dto.response.ListHotelResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface HotelMapper {
    @Mapping(target = "account", ignore = true)
    Hotel toEntity(HotelCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "account", ignore = true)
    void updateEntity(@MappingTarget Hotel hotel, HotelUpdateRequest req);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(target = "amenities", ignore = true)
    @Mapping(target = "services", ignore = true)
    ListHotelResponse toResponse(Hotel hotel);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(target = "amenities", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "images", ignore = true)
    HotelDetailResponse toDetailResponse(Hotel hotel);
}
