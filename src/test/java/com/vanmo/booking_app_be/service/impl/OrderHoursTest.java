package com.vanmo.booking_app_be.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vanmo.booking_app_be.dto.response.BookingHoursResponse;

class OrderHoursTest {

    // a Monday
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

    @Test
    void countsOrdersByWeekdayAndHourAndFindsTheBusiestSlot() {
        BookingHoursResponse report = OrderHours.hours(
                MONDAY,
                MONDAY.plusDays(6),
                List.of(
                        at(MONDAY, 9),
                        at(MONDAY, 9),
                        at(MONDAY.plusDays(5), 21), // Saturday
                        at(MONDAY.plusDays(5), 23),
                        at(MONDAY.plusDays(6), 2))); // Sunday night

        assertThat(report.getOrders()).isEqualTo(5);
        assertThat(report.getCells()).hasSize(7);
        assertThat(report.getCells().get(0).get(9)).isEqualTo(2L);
        assertThat(report.getCells().get(5).get(21)).isEqualTo(1L);
        assertThat(report.getByHour().get(9)).isEqualTo(2L);
        assertThat(report.getBusiestWeekday()).isEqualTo(1);
        assertThat(report.getBusiestHour()).isEqualTo(9);
        // 23h and 2h
        assertThat(report.getNightShare()).isEqualTo(40.0);
    }

    @Test
    void noOrdersMeansNoBusiestSlot() {
        BookingHoursResponse report = OrderHours.hours(MONDAY, MONDAY, List.of());

        assertThat(report.getBusiestHour()).isNull();
        assertThat(report.getNightShare()).isZero();
        assertThat(report.getCells().get(6)).hasSize(24);
    }

    private static Date at(LocalDate day, int hour) {
        return Date.from(LocalDateTime.of(day, LocalTime.of(hour, 15))
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }
}
