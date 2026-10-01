package com.vanmo.booking_app_be.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourBookingResponse {
    Long id;
    /** Null once the tour itself was deleted; the name and prices below are kept. */
    Long tourId;

    String tourName;
    String tourPathImage;
    String tourDuration;
    String tourDeparture;
    Long hotelId;
    String hotelName;
    String hotelPhone;
    Long customerId;
    String customerName;
    String customerPhone;
    Long conversationId;
    LocalDate tourDate;
    int guests;
    BigDecimal unitPrice;
    BigDecimal totalAmount;
    TourBookingStatus status;
    String note;
    String cancelReason;
    ChatSide canceledBy;
    boolean createdByAi;
    Date onCreate;
    Date onUpdate;
}
