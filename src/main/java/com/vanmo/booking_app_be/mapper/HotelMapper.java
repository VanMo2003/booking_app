package com.vanmo.booking_app_be.mapper;

import com.vanmo.booking_app_be.dto.request.HotelCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelUpdateRequest;
import com.vanmo.booking_app_be.dto.response.AmenityResponse;
import com.vanmo.booking_app_be.dto.response.HotelResponse;
import com.vanmo.booking_app_be.dto.response.ServiceResponse;
import com.vanmo.booking_app_be.entity.Hotel;
import org.mapstruct.*;

import java.util.List;

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
    HotelResponse toResponse(Hotel hotel);
}
