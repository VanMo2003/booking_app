package com.vanmo.booking_app_be.event;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.vanmo.booking_app_be.repository.DeviceTokenRepository;
import com.vanmo.booking_app_be.service.PushNotificationSender;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PushNotificationListener {

    private final PushNotificationSender pushNotificationSender;
    private final DeviceTokenRepository deviceTokenRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPushRequested(PushRequestedEvent event) {
        if (!pushNotificationSender.isEnabled() || event.accountIds().isEmpty()) {
            return;
        }
        List<String> tokens = deviceTokenRepository.findTokensByAccountIds(event.accountIds()).stream()
                .distinct()
                .toList();
        if (tokens.isEmpty()) {
            return;
        }
        List<String> stale = pushNotificationSender.send(tokens, event.title(), event.body(), event.data());
        if (!stale.isEmpty()) {
            deviceTokenRepository.deleteAllByTokens(stale);
        }
    }
}
