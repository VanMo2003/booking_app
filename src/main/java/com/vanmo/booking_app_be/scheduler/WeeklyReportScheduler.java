package com.vanmo.booking_app_be.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vanmo.booking_app_be.service.ReportService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Monday 8:00 (server time): last week's figures to every chain owner and branch manager. */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WeeklyReportScheduler {

    ReportService reportService;

    @Scheduled(cron = "0 0 8 * * MON")
    public void sendWeeklyDigests() {
        reportService.sendWeeklyDigests();
    }
}
