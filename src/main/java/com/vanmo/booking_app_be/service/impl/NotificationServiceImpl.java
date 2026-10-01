package com.vanmo.booking_app_be.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.transaction.Transactional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.vanmo.booking_app_be.dto.request.DeviceTokenRequest;
import com.vanmo.booking_app_be.dto.response.NotificationCountResponse;
import com.vanmo.booking_app_be.dto.response.NotificationResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.DeviceToken;
import com.vanmo.booking_app_be.entity.Notification;
import com.vanmo.booking_app_be.entity.enums.NotificationType;
import com.vanmo.booking_app_be.entity.enums.Role;
import com.vanmo.booking_app_be.event.PushRequestedEvent;
import com.vanmo.booking_app_be.exception.ResourceNotFoundException;
import com.vanmo.booking_app_be.mapper.NotificationMapper;
import com.vanmo.booking_app_be.repository.AccountRepository;
import com.vanmo.booking_app_be.repository.DeviceTokenRepository;
import com.vanmo.booking_app_be.repository.NotificationRepository;
import com.vanmo.booking_app_be.security.AccessGuard;
import com.vanmo.booking_app_be.service.NotificationService;
import com.vanmo.booking_app_be.utils.NotificationMessages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final AccountRepository accountRepository;
    private final NotificationMapper notificationMapper;
    private final AccessGuard accessGuard;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void notify(
            Account account,
            NotificationType type,
            NotificationMessages.Message message,
            String link,
            Long referenceId) {
        send(List.of(account), type, message, link, referenceId);
    }

    @Override
    public void notifyAdmins(
            NotificationType type, NotificationMessages.Message message, String link, Long referenceId) {
        send(accountRepository.findAllByRole(Role.ADMIN), type, message, link, referenceId);
    }

    @Override
    public void notifyAccounts(
            List<Account> accounts,
            NotificationType type,
            NotificationMessages.Message message,
            String link,
            Long referenceId) {
        send(accounts, type, message, link, referenceId);
    }

    private void send(
            List<Account> recipients,
            NotificationType type,
            NotificationMessages.Message message,
            String link,
            Long referenceId) {
        if (recipients.isEmpty()) {
            return;
        }
        notificationRepository.saveAll(recipients.stream()
                .map(account -> Notification.builder()
                        .account(account)
                        .type(type)
                        .title(message.title())
                        .body(message.body())
                        .link(link)
                        .referenceId(referenceId)
                        .build())
                .toList());

        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        if (link != null) {
            data.put("link", link);
        }
        if (referenceId != null) {
            data.put("referenceId", referenceId.toString());
        }
        // Delivered after commit (PushNotificationListener), so a rolled-back review never reaches a phone.
        eventPublisher.publishEvent(new PushRequestedEvent(
                recipients.stream().map(Account::getId).toList(), message.title(), message.body(), data));
    }

    @Override
    public PageResponse<NotificationResponse> getMine(int page, int size) {
        Account current = accessGuard.currentAccount();
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "onCreate").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(notificationRepository
                .findAllByAccount_Id(current.getId(), pageable)
                .map(notificationMapper::toResponse));
    }

    @Override
    public NotificationCountResponse countUnread() {
        Account current = accessGuard.currentAccount();
        return NotificationCountResponse.builder()
                .unread(notificationRepository.countByAccount_IdAndReadFalse(current.getId()))
                .build();
    }

    @Override
    public NotificationResponse markRead(Long id) {
        Account current = accessGuard.currentAccount();
        Notification notification = notificationRepository
                .findByIdAndAccount_Id(id, current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found id=" + id));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }
        return notificationMapper.toResponse(notification);
    }

    @Override
    public void markAllRead() {
        notificationRepository.markAllRead(accessGuard.currentAccount().getId());
    }

    @Transactional
    @Override
    public void registerDevice(DeviceTokenRequest req) {
        Account current = accessGuard.currentAccount();
        String token = req.getToken().trim();
        // A device belongs to whoever signed in on it last.
        DeviceToken device = deviceTokenRepository
                .findByToken(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());
        device.setAccount(current);
        device.setPlatform(req.getPlatform());
        deviceTokenRepository.save(device);
    }

    @Override
    public void unregisterDevice(String token) {
        Account current = accessGuard.currentAccount();
        deviceTokenRepository
                .findByTokenAndAccount_Id(token.trim(), current.getId())
                .ifPresent(deviceTokenRepository::delete);
    }
}
