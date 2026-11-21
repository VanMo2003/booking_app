package com.vanmo.booking_app_be.entity;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;

import com.vanmo.booking_app_be.entity.enums.NotificationType;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** In-app notification inbox row. The same title/body is pushed to the account's devices through FCM. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    NotificationType type;

    @NotNull
    String title;

    @Column(length = 2000)
    String body;

    /** App deep link opened when the notification is tapped, e.g. {@code bookingapp://owner-status}. */
    String link;

    /** Id of the record the notification is about (the hotel chain id for owner registrations). */
    Long referenceId;

    // "read" is a reserved word in MySQL.
    @Column(name = "is_read")
    boolean read;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;
}
