package com.vanmo.booking_app_be.utils;

/** Deep links the Flutter app handles ({@code bookingapp://...}), opened from push notifications and emails. */
public final class AppLinks {

    public static final String SCHEME = "bookingapp";
    public static final String OWNER_STATUS = "owner-status";
    public static final String NOTIFICATIONS = "notifications";

    private AppLinks() {}

    public static String deepLink(String target) {
        return SCHEME + "://" + target;
    }

    public static String ownerStatus() {
        return deepLink(OWNER_STATUS);
    }

    public static String ownerRegistration(Long chainId) {
        return deepLink("owner-registrations/" + chainId);
    }

    public static String chat(Long conversationId) {
        return deepLink("chat/" + conversationId);
    }
}
