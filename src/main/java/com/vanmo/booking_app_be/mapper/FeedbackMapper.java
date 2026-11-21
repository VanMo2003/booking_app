package com.vanmo.booking_app_be.mapper;

import org.mapstruct.*;

import com.vanmo.booking_app_be.dto.request.FeedbackCreateRequest;
import com.vanmo.booking_app_be.dto.request.FeedbackUpdateRequest;
import com.vanmo.booking_app_be.dto.response.FeedbackResponse;
import com.vanmo.booking_app_be.entity.Feedback;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    Feedback toEntity(FeedbackCreateRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Feedback entity, FeedbackUpdateRequest req);

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "hotelId", source = "booking.hotel.id")
    @Mapping(target = "hotelName", source = "booking.hotel.name")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.fullName")
    FeedbackResponse toResponse(Feedback entity);
}
