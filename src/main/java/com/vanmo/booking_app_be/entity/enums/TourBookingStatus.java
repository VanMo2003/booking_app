package com.vanmo.booking_app_be.entity.enums;

/** PENDING (waiting for the branch) → CONFIRMED → COMPLETED; PENDING or CONFIRMED → CANCELED by either side. */
public enum TourBookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELED
}
