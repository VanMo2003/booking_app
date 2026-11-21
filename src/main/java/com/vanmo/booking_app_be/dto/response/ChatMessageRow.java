package com.vanmo.booking_app_be.dto.response;

import java.util.Date;

import com.vanmo.booking_app_be.entity.enums.ChatSide;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Who wrote one chat message and when, for the assistant report. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageRow {
    Long conversationId;
    ChatSide senderSide;
    Boolean aiGenerated;
    Date onCreate;
}
