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

    /** Of the guests, how many are children (child price); the rest are adults. */
    @Builder.Default
    @Column(columnDefinition = "int default 0")
    int children = 0;

    /** Price per child when the order was placed. */
    @Column(precision = 12)
    BigDecimal childUnitPrice;

    /** Voucher used on the order; its use is given back if the order is cancelled. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    Voucher voucher;

    @Column(length = 30)
    String voucherCode;

    /** Taken off the tour amount by the voucher (tourAmount is after it). */
    @Column(precision = 14)
    BigDecimal voucherDiscount;

    /** Price per guest when the order was placed. */
    @NotNull
    @Column(nullable = false, precision = 12)
    BigDecimal unitPrice;

    /** unitPrice × guests. */
    @Column(precision = 14)
    BigDecimal tourAmount;

    /** The rooms' nights; zero for a tour without rooms. */
    @Column(precision = 14)
    BigDecimal roomAmount;

    /** tourAmount + roomAmount. */
    @NotNull
    @Column(nullable = false, precision = 14)
    BigDecimal totalAmount;

    /**
     * The room booking made with the order (check-in on the tour date, the tour's nights, the rooms the guest chose).
     * It is a normal {@link Booking}, so the rooms are blocked for others and the front desk checks the guest in and
     * out as usual; confirming or cancelling the tour order does the same to it. Null for a tour without rooms.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    Booking booking;

    /** End of the stay (the stay starts on the tour date); null without rooms. */
    LocalDate stayCheckout;

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
