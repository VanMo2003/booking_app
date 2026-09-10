package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.HotelChainCreateRequest;
import com.vanmo.booking_app_be.dto.request.HotelChainUpdateRequest;
import com.vanmo.booking_app_be.dto.response.HotelChainDetailResponse;
import com.vanmo.booking_app_be.dto.response.ListHotelChainResponse;
import com.vanmo.booking_app_be.entity.HotelChain;

@Mapper(componentModel = "spring")
public interface HotelChainMapper {
    @Mapping(target = "account", ignore = true)
    HotelChain toEntity(HotelChainCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "account", ignore = true)
    void updateEntity(@MappingTarget HotelChain hotelChain, HotelChainUpdateRequest req);

    @Mapping(source = "account.id", target = "accountId")
    ListHotelChainResponse toResponse(HotelChain hotelChain);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(target = "hotels", ignore = true)
    HotelChainDetailResponse toDetailResponse(HotelChain hotelChain);
}
