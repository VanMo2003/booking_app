package com.vanmo.booking_app_be.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/** The FCM v1 message the app relies on to show notifications while it is in the background or closed. */
class FcmPushNotificationSenderTest {

    @Test
    @SuppressWarnings("unchecked")
    void chatMessageGoesToTheHighPriorityChannelAndReplacesTheThreadsLastOne() throws Exception {
        Map<String, Object> message = FcmPushNotificationSender.message(
                "device-token",
                "Khách Sạn Hoàn Kiếm Palace",
                "Dạ em chào anh/chị",
                Map.of("type", "CHAT_MESSAGE", "link", "bookingapp://chat/18", "tag", "chat-18"));

        Map<String, Object> notification = (Map<String, Object>) message.get("notification");
        assertEquals("Khách Sạn Hoàn Kiếm Palace", notification.get("title"));
        Map<String, Object> android = (Map<String, Object>) message.get("android");
        assertEquals("high", android.get("priority"));
        assertEquals("chat-18", android.get("collapse_key"));
        Map<String, Object> androidNotification = (Map<String, Object>) android.get("notification");
        assertEquals(FcmPushNotificationSender.ANDROID_CHANNEL_ID, androidNotification.get("channel_id"));
        assertEquals("PRIORITY_HIGH", androidNotification.get("notification_priority"));
        assertEquals("chat-18", androidNotification.get("tag"));
        assertEquals("bookingapp://chat/18", ((Map<String, String>) message.get("data")).get("link"));

        // Lets a manual check send the exact payload to FCM with validate_only (see -Dfcm.sample.out).
        String sampleOut = System.getProperty("fcm.sample.out");
        if (sampleOut != null) {
            Files.writeString(Path.of(sampleOut), new ObjectMapper().writeValueAsString(message));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void notificationWithoutTagStillUsesTheChannel() {
        Map<String, Object> message = FcmPushNotificationSender.message(
                "device-token",
                "Đơn tour mới #5",
                null,
                Map.of("type", "TOUR_BOOKING_CREATED", "link", "bookingapp://tour-bookings/5"));

        Map<String, Object> android = (Map<String, Object>) message.get("android");
        assertFalse(android.containsKey("collapse_key"));
        assertEquals(
                FcmPushNotificationSender.ANDROID_CHANNEL_ID,
                ((Map<String, Object>) android.get("notification")).get("channel_id"));
        assertFalse(((Map<String, Object>) message.get("notification")).containsKey("body"));
    }
}
