package com.vanmo.booking_app_be.event;

import com.vanmo.booking_app_be.entity.HotelChain;

/** An admin approved or rejected an owner registration; carries what the result email needs. */
public record OwnerReviewedEvent(
        boolean approved, String email, String ownerName, String ownerUsername, String hotelName, String reason) {

    public static OwnerReviewedEvent of(HotelChain chain) {
        return new OwnerReviewedEvent(
                chain.isApproved(),
                chain.getEmail(),
                chain.getOwnerName(),
                chain.getAccount().getUsername(),
                chain.getName(),
                chain.getRejectionReason());
    }
}
