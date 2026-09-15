package com.vanmo.booking_app_be.service;

import com.vanmo.booking_app_be.event.OwnerReviewedEvent;

public interface MailService {
    /** Emails the owner the review result with a link that opens the app. Never throws; failures are logged. */
    void sendOwnerReview(OwnerReviewedEvent review);
}
