package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.vanmo.booking_app_be.entity.enums.DiscountType;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * A discount code a chain gives its guests for tour orders, valid at every branch of the chain or at one. Uses are
 * counted when an order is placed and given back when it is cancelled.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Upper case, unique across the platform: what the guest types. */
    @Column(nullable = false, unique = true, length = 30)
    String code;

    @Column(nullable = false)
    String title;

    @Column(length = 1000)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    DiscountType discountType;

    /** Percent (1-90) or amount in đồng. */
    @Column(nullable = false, precision = 12)
    BigDecimal value;

    /** Cap for a percentage voucher; null = no cap. */
    @Column(precision = 12)
    BigDecimal maxDiscount;

    /** Smallest tour amount the voucher applies to; null = any. */
    @Column(precision = 12)
    BigDecimal minOrderAmount;

    @Column(nullable = false)
    LocalDate startsAt;

    @Column(nullable = false)
    LocalDate endsAt;

    /** Uses across all guests; null = unlimited. */
    Integer usageLimit;

    @Builder.Default
    int usedCount = 0;

    /** Open or completed orders a guest may have with this voucher. */
    @Builder.Default
    int perCustomerLimit = 1;

    @Builder.Default
    boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_chain_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    HotelChain hotelChain;

    /** Only this branch; null = every branch of the chain. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    Hotel hotel;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;
}
