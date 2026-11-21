package com.vanmo.booking_app_be.service;

import java.util.List;
import java.util.Map;

public interface PushNotificationSender {
    /** False when no Firebase service account is configured; notifications then stay in-app only. */
    boolean isEnabled();

    /** Sends one message per token and returns the tokens FCM reports as no longer registered. */
    List<String> send(List<String> tokens, String title, String body, Map<String, String> data);
}
