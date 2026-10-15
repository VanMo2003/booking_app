package com.vanmo.booking_app_be.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.vanmo.booking_app_be.dto.common.ApiResponse;
import com.vanmo.booking_app_be.dto.request.DeviceTokenRequest;
import com.vanmo.booking_app_be.dto.response.NotificationCountResponse;
import com.vanmo.booking_app_be.dto.response.NotificationResponse;
import com.vanmo.booking_app_be.dto.response.PageResponse;
import com.vanmo.booking_app_be.dto.response.TestPushResponse;
import com.vanmo.booking_app_be.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getMine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.getMine(page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationCountResponse> countUnread() {
        return ApiResponse.ok(notificationService.countUnread());
    }

    @PutMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok(null);
    }

    /** Registers this device's FCM token for the signed-in account. */
    @PostMapping("/devices")
    public ApiResponse<Void> registerDevice(@Valid @RequestBody DeviceTokenRequest req) {
        notificationService.registerDevice(req);
        return ApiResponse.ok(null);
    }

    /** Test push to the caller's own devices after a delay (default 10 s), to check delivery with the app closed. */
    @PostMapping("/devices/test")
    public ApiResponse<TestPushResponse> sendTestPush(@RequestParam(defaultValue = "10") int delaySeconds) {
        return ApiResponse.ok(notificationService.sendTestPush(delaySeconds));
    }

    /** Called before logout so the device stops receiving this account's pushes. */
    @DeleteMapping("/devices")
    public ApiResponse<Void> unregisterDevice(@RequestParam String token) {
        notificationService.unregisterDevice(token);
        return ApiResponse.ok(null);
    }
}
