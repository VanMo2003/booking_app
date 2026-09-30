package com.vanmo.booking_app_be.dto.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.ChatSide;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A conversation as one side sees it: {@code unread} and {@code mySide} depend on who asks. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationResponse {
    Long id;

    Long hotelId;
    String hotelName;
    String hotelAddress;
    String hotelPhone;
    String hotelPathImage;

    Long customerId;
    String customerName;
    String customerPhone;
    String customerPathImage;

    String lastMessage;
    ChatSide lastSenderSide;
    Date lastMessageAt;

    ChatSide mySide;
    int unread;

    /** The assistant handed the thread to the team; only meaningful on the hotel side. */
    Boolean needsStaff;

    String handoffReason;

    Date onCreate;
}
