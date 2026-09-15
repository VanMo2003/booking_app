package com.vanmo.booking_app_be.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.vanmo.booking_app_be.service.MailService;

import lombok.RequiredArgsConstructor;

/** Emails the review result off the request thread, and only once the review is actually committed. */
@Component
@RequiredArgsConstructor
public class OwnerReviewMailListener {

    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOwnerReviewed(OwnerReviewedEvent event) {
        mailService.sendOwnerReview(event);
    }
}
