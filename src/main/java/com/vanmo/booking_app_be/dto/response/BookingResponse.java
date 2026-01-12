package com.vanmo.booking_app_be.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {
    Long id;
    String checkinDate;
    String checkoutDate;
    String bookingStatus;
    String paymentMethod;
    ListHotelResponse hotel;
    CustomerResponse customer;
    List<RoomResponse> rooms;
    List<ServiceResponse> services;
    BigDecimal totalAmount;
    String note;
    Date onCreate;
    Date onUpdate;

}
