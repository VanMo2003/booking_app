package com.vanmo.booking_app_be.entity;

import java.util.Date;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.vanmo.booking_app_be.entity.enums.ChatSide;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * One chat thread between a customer and one branch. The branch side is a shared inbox: its owner, manager and staff
 * all see and answer the same thread, so read state is kept per side, not per person. Removed together with the
 * branch or the customer (database-level cascade).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "hotel_id"}))
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Hotel hotel;

    /** Short preview of the latest message for inbox lists. */
    @Column(length = 255)
    String lastMessage;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    ChatSide lastSenderSide;

    Date lastMessageAt;

    /** Messages from the hotel the customer hasn't opened yet. */
    int customerUnread;

    /** Messages from the customer nobody on the branch team has opened yet. */
    int hotelUnread;

    /** The assistant handed this thread to the team (booking change, complaint…); cleared when staff replies. */
    Boolean needsStaff;

    @Column(length = 255)
    String handoffReason;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;
}
