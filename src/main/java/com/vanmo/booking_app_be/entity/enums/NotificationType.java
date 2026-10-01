package com.vanmo.booking_app_be.entity.enums;

public enum NotificationType {
    /** Sent to admins when an owner submits (or resubmits) a registration. */
    OWNER_REGISTERED,
    OWNER_APPROVED,
    OWNER_REJECTED,
    /** Sent to the branch team when a guest (or the AI receptionist for them) places a tour order. */
    TOUR_BOOKING_CREATED,
    TOUR_BOOKING_CONFIRMED,
    /** Sent to the other side: the guest when the branch cancels, the branch team when the guest does. */
    TOUR_BOOKING_CANCELED
}
