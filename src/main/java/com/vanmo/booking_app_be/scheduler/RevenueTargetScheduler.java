package com.vanmo.booking_app_be.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vanmo.booking_app_be.service.ReportService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Every morning at 8:00 (server time): tell branches that reached their month's goal, or are falling behind it. */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RevenueTargetScheduler {

    ReportService reportService;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendRevenueTargetAlerts() {
        reportService.sendRevenueTargetAlerts();
    }
}
