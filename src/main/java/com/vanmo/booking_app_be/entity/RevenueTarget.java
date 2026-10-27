package com.vanmo.booking_app_be.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * A branch's revenue goal for one calendar month, set by its chain owner or manager. Compared with the month's total
 * revenue (completed bookings and tours) on the dashboards. Removed with the branch.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "revenue_targets", uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "month"}))
public class RevenueTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Hotel hotel;

    /** First day of the month the goal is for. */
    @NotNull
    @Column(nullable = false)
    LocalDate month;

    @NotNull
    @Column(nullable = false, precision = 14)
    BigDecimal amount;

    /** The "goal reached" alert went out for this goal; cleared when the amount changes. */
    @Builder.Default
    boolean reachedNotified = false;

    /** The "goal at risk" alert went out for this goal; cleared when the amount changes. */
    @Builder.Default
    boolean atRiskNotified = false;

    @UpdateTimestamp
    Date onUpdate;
}
