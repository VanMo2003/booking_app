package com.vanmo.booking_app_be.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "feedbacks",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"booking_id", "customer_id"})})
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    Integer rating;

    @Column(length = 5000, columnDefinition = "TEXT")
    String comments;

    @NotNull
    LocalDate reviewDate;

    /** The branch's public answer to this review; null until someone on the branch writes one. */
    @Column(length = 2000, columnDefinition = "TEXT")
    String reply;

    LocalDateTime replyAt;

    /** Who answered. Kept as SET NULL so deleting a manager's account never deletes the reply. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by_account_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    Account repliedBy;
}
