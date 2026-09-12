package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
    String paymentStatus;
    LocalDateTime paymentExpireAt;
    LocalDateTime paidAt;
    HotelInfoResponse hotel;
    CustomerInfoResponse customer;
    List<BookingRoomResponse> bookingRooms;
    List<BookingServiceResponse> bookingServices;
    BigDecimal totalAmount;
    String note;
    /** Null when the customer booked it themselves; set when staff took it at the front desk. */
    String handledByAccountId;

    Date onCreate;
    Date onUpdate;
}
