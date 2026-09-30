package com.vanmo.booking_app_be.entity;

import java.util.Date;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.vanmo.booking_app_be.entity.enums.ChatSide;
import com.vanmo.booking_app_be.entity.enums.Role;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * A message in a {@link Conversation}. The sender is stored as a snapshot (account id, name, role) without a foreign
 * key, so the history stays readable after a staff account is deleted.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "chat_messages",
        indexes = @Index(name = "idx_chat_messages_conversation", columnList = "conversation_id, id"))
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    ChatSide senderSide;

    @Column(name = "sender_account_id", length = 36)
    String senderAccountId;

    String senderName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    Role senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    /** Written by the chat assistant on the hotel's behalf (no sender account). */
    Boolean aiGenerated;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;
}
