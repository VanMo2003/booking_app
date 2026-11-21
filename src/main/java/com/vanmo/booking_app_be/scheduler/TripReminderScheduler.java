package com.vanmo.booking_app_be.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vanmo.booking_app_be.service.TripReminderService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/** 18:00 (Vietnam time) every day: remind guests of tomorrow's tours and check-ins. */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripReminderScheduler {

    TripReminderService tripReminderService;

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Ho_Chi_Minh")
    public void remind() {
        tripReminderService.sendTomorrowReminders();
    }
}
