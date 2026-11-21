package com.vanmo.booking_app_be.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.LongStream;

import com.vanmo.booking_app_be.dto.response.BookingHoursResponse;

/** Pure arithmetic of the order-hours report, kept apart for unit tests. */
final class OrderHours {
    /** 22h to 5h59 counts as night. */
    static final int NIGHT_FROM = 22;

    static final int NIGHT_TO = 6;

    private OrderHours() {}

    static BookingHoursResponse hours(LocalDate from, LocalDate to, List<Date> createdAt) {
        long[][] cells = new long[7][24];
        long orders = 0;
        long night = 0;
        for (Date created : createdAt) {
            if (created == null) {
                continue;
            }
            LocalDateTime at =
                    created.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            int hour = at.getHour();
            cells[at.getDayOfWeek().getValue() - 1][hour]++;
            orders++;
            if (hour >= NIGHT_FROM || hour < NIGHT_TO) {
                night++;
            }
        }
        Integer busiestWeekday = null;
        Integer busiestHour = null;
        long most = 0;
        List<List<Long>> rows = new ArrayList<>();
        long[] byHour = new long[24];
        for (int day = 0; day < 7; day++) {
            rows.add(LongStream.of(cells[day]).boxed().toList());
            for (int hour = 0; hour < 24; hour++) {
                byHour[hour] += cells[day][hour];
                if (cells[day][hour] > most) {
                    most = cells[day][hour];
                    busiestWeekday = day + 1;
                    busiestHour = hour;
                }
            }
        }
        return BookingHoursResponse.builder()
                .from(from)
                .to(to)
                .orders(orders)
                .cells(rows)
                .byHour(LongStream.of(byHour).boxed().toList())
                .busiestWeekday(busiestWeekday)
                .busiestHour(busiestHour)
                .nightShare(RevenueFigures.rate(night, orders))
                .build();
    }
}
