package com.vanmo.booking_app_be.dto.response;

import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** How many tour orders are in one status (and, for cancelled ones, who cancelled). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourStatusCountRow {
    TourBookingStatus status;
    ChatSide canceledBy;
    Long count;
}
