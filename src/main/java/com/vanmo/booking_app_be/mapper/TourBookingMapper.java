package com.vanmo.booking_app_be.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vanmo.booking_app_be.dto.response.TourBookingResponse;
import com.vanmo.booking_app_be.entity.TourBooking;

@Mapper(componentModel = "spring")
public interface TourBookingMapper {
    @Mapping(source = "tour.id", target = "tourId")
    @Mapping(source = "tour.pathImage", target = "tourPathImage")
    @Mapping(source = "tour.duration", target = "tourDuration")
    @Mapping(source = "tour.departure", target = "tourDeparture")
    @Mapping(source = "hotel.id", target = "hotelId")
    @Mapping(source = "hotel.name", target = "hotelName")
    @Mapping(source = "hotel.phone", target = "hotelPhone")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.fullName", target = "customerName")
    @Mapping(source = "customer.phoneNumber", target = "customerPhone")
    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(target = "rooms", ignore = true)
    TourBookingResponse toResponse(TourBooking booking);
}
