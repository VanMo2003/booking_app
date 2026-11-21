package com.vanmo.booking_app_be.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class NotificationMessagesTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

    @Test
    void weeklyReportSaysHowBookedTheComingWeekIs() {
        NotificationMessages.Message message = NotificationMessages.weeklyReport(
                "Hoàn Kiếm", MONDAY, MONDAY.plusDays(6), BigDecimal.valueOf(12_500_000), 25.0, 41.6, 9, 1, 37.4);

        assertThat(message.body())
                .contains("12.500.000 ₫ (+25% so với tuần trước)")
                .contains("lấp đầy 42%")
                .endsWith("Tuần này đã đặt 37% phòng.");
    }

    @Test
    void withoutRoomsTheComingWeekIsLeftOut() {
        NotificationMessages.Message message = NotificationMessages.weeklyReport(
                "Chuỗi", MONDAY, MONDAY.plusDays(6), BigDecimal.ZERO, null, 0, 0, 0, null);

        assertThat(message.body()).doesNotContain("đã đặt").doesNotContain("so với tuần trước");
    }
}
