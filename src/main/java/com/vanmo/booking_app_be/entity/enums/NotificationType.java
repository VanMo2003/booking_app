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
    TOUR_BOOKING_CANCELED,
    /** Sent to the chain owner and branch manager the morning a branch reaches its month's revenue goal. */
    REVENUE_TARGET_REACHED,
    /** Sent to the chain owner and branch manager once, from mid-month, when the month projects below 80% of goal. */
    REVENUE_TARGET_AT_RISK,
    /** Monday 8:00 digest of the week before: chain owners get their chain, managers their branch. */
    WEEKLY_REPORT,
    /** Sent to the chain owner and branch manager when a guest leaves (or lowers a review to) 2 stars or fewer. */
    LOW_RATING_REVIEW
}
