package com.vanmo.booking_app_be.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/** A chat thread in one note for the branch team: what the guest came for, what is open, how urgent it looks. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSummaryResponse {

    Long conversationId;

    String summary;

    /** Still open, one line each. */
    List<String> openItems;

    List<String> nextSteps;

    /** LOW, NORMAL or HIGH. */
    String urgency;

    /** The guest wrote last, so they are waiting on the hotel - worked out from the thread, not by the model. */
    boolean waitingOnHotel;

    /** The assistant already flagged the thread for a person. */
    boolean needsStaff;

    String handoffReason;

    /** How many messages the summary was written from. */
    int messageCount;
}
