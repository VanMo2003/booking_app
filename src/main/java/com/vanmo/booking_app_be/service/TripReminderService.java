package com.vanmo.booking_app_be.service;

/** Evening reminders to guests about tomorrow's tours and check-ins. */
public interface TripReminderService {
    /** No caller check: reached from the scheduler only. Returns how many reminders went out. */
    int sendTomorrowReminders();
}
