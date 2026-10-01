package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.TourBookingStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * A guest's order for one of a branch's tours, paid at the hotel. The tour name and price are copied in when it is
 * placed, so editing or deleting the tour later never changes what the guest agreed to (the tour link becomes null).
 * Removed together with the branch or the customer.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "tour_bookings")
public class TourBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    Tour tour;

    @NotNull
    @Column(nullable = false)
    String tourName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Customer customer;

    /** Chat thread the AI receptionist took the order in; null for an order placed in the app. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    Conversation conversation;

    @NotNull
    @Column(nullable = false)
    LocalDate tourDate;

    @Column(nullable = false)
    int guests;

    /** Price per guest when the order was placed. */
    @NotNull
    @Column(nullable = false, precision = 12)
    BigDecimal unitPrice;

    @NotNull
    @Column(nullable = false, precision = 14)
    BigDecimal totalAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    TourBookingStatus status;

    @Column(length = 500)
    String note;

    @Column(length = 255)
    String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    ChatSide canceledBy;

    /** Team member who last confirmed, completed or cancelled it. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_account_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    Account handledBy;

    /** Placed by the AI receptionist after the guest agreed in chat. */
    boolean createdByAi;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;
}
