package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * When orders were placed over [from, to]: room bookings and tour orders by the weekday and hour they were created
 * (server time), whatever became of them - to plan the front desk and the chat.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingHoursResponse {
    LocalDate from;
    LocalDate to;
    long orders;

    /** 7 rows (Monday ... Sunday) of 24 counts (0h ... 23h). */
    List<List<Long>> cells;

    /** Orders per hour of the day, all weekdays together. */
    List<Long> byHour;

    /** The busiest weekday (1 = Monday) and hour; null without orders. */
    Integer busiestWeekday;

    Integer busiestHour;

    /** Orders placed from 22h to 6h, percentage - when nobody may be at the desk. */
    double nightShare;
}
