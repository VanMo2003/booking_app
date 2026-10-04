package com.vanmo.booking_app_be.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.vanmo.booking_app_be.service.PushNotificationSender;

import lombok.extern.slf4j.Slf4j;

/**
 * Firebase Cloud Messaging over the HTTP v1 API, authenticated with a service account JSON key
 * ({@code FIREBASE_CREDENTIALS}). Without that key the sender is disabled and notifications stay in-app only.
 */
@Slf4j
@Component
public class FcmPushNotificationSender implements PushNotificationSender {

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    /** Must match the channel the Flutter app creates in MainActivity.kt. */
    static final String ANDROID_CHANNEL_ID = "booking_app_alerts";

    private final RestClient restClient = RestClient.create();
    private final GoogleCredentials credentials;
    private final String projectId;

    public FcmPushNotificationSender(@Value("${firebase.credentials-path:}") String credentialsPath) {
        ServiceAccountCredentials serviceAccount = load(credentialsPath);
        this.credentials = serviceAccount == null ? null : serviceAccount.createScoped(List.of(SCOPE));
        this.projectId = serviceAccount == null ? null : serviceAccount.getProjectId();
    }

    @Override
    public boolean isEnabled() {
        return credentials != null && projectId != null;
    }

    @Override
    public List<String> send(List<String> tokens, String title, String body, Map<String, String> data) {
        String accessToken = accessToken();
        if (accessToken == null) {
            return List.of();
        }
        String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";
        List<String> stale = new ArrayList<>();
        for (String token : tokens) {
            try {
                restClient
                        .post()
                        .uri(url)
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("message", message(token, title, body, data)))
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 404
                        || e.getResponseBodyAsString().contains("UNREGISTERED")) {
                    stale.add(token);
                } else {
                    log.warn("FCM rejected a message ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
                }
            } catch (RestClientException e) {
                log.warn("Cannot reach FCM: {}", e.getMessage());
            }
        }
        return stale;
    }

    static Map<String, Object> message(String token, String title, String body, Map<String, String> data) {
        Map<String, String> notification = new HashMap<>();
        notification.put("title", title);
        if (body != null) {
            notification.put("body", body);
        }
        Map<String, Object> android = new HashMap<>();
        // High priority wakes a phone in Doze, so the notification shows while the app is in the background or closed.
        android.put("priority", "high");
        Map<String, Object> androidNotification = new HashMap<>();
        // Created by the app (MainActivity) with high importance: pops up on screen with sound, not silently in the
        // tray.
        androidNotification.put("channel_id", ANDROID_CHANNEL_ID);
        androidNotification.put("notification_priority", "PRIORITY_HIGH");
        androidNotification.put("default_sound", true);
        // A "tag" groups notifications on Android: a newer one with the same tag replaces the older (chat threads).
        if (data != null && data.get("tag") != null) {
            androidNotification.put("tag", data.get("tag"));
            android.put("collapse_key", data.get("tag"));
        }
        android.put("notification", androidNotification);
        return Map.of(
                "token",
                token,
                "notification",
                notification,
                "data",
                data == null ? Map.of() : data,
                "android",
                android,
                "apns",
                Map.of("payload", Map.of("aps", Map.of("sound", "default"))));
    }

    private synchronized String accessToken() {
        try {
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            log.error("Cannot obtain an FCM access token", e);
            return null;
        }
    }

    private static ServiceAccountCredentials load(String path) {
        if (path == null || path.isBlank()) {
            log.info("FIREBASE_CREDENTIALS is not set - push notifications are off, in-app notifications still work");
            return null;
        }
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            return ServiceAccountCredentials.fromStream(in);
        } catch (IOException | RuntimeException e) {
            log.error("Cannot read the Firebase service account at {} - push notifications are off", path, e);
            return null;
        }
    }
}
