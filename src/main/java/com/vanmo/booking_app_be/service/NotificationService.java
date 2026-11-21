package com.vanmo.booking_app_be.service;

import java.util.List;

import com.vanmo.booking_app_be.dto.request.DeviceTokenRequest;
import com.vanmo.booking_app_be.dto.response.NotificationCountResponse;
import com.vanmo.booking_app_be.dto.response.NotificationResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.TestPushResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.utils.NotificationMessages;

public interface NotificationService {
    /** Stores an in-app notification and pushes it to the account's devices once the transaction commits. */
    void notify(
            Account account,
            NotificationType type,
            NotificationMessages.Message message,
            String link,
            Long referenceId);

    void notifyAdmins(NotificationType type, NotificationMessages.Message message, String link, Long referenceId);

    /** {@link #notify} for several accounts at once, e.g. a branch's whole team. */
    void notifyAccounts(
            List<Account> accounts,
            NotificationType type,
            NotificationMessages.Message message,
            String link,
            Long referenceId);

    PageResponse<NotificationResponse> getMine(int page, int size);

    NotificationCountResponse countUnread();

    NotificationResponse markRead(Long id);

    void markAllRead();

    void registerDevice(DeviceTokenRequest req);

    /**
     * Pushes a test notification to the caller's own devices after {@code delaySeconds} (0-60), so they can close
     * the app and check that notifications still arrive. Push only - nothing is added to the inbox.
     */
    TestPushResponse sendTestPush(int delaySeconds);

    void unregisterDevice(String token);
}
