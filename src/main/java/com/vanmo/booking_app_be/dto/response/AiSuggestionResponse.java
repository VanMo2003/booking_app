package com.vanmo.booking_app_be.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** A draft reply for a team member to review; nothing is sent to the guest. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiSuggestionResponse {
    String content;

    /** The assistant judged this needs a person (booking change, complaint, missing info). */
    boolean needsStaff;

    String handoffReason;

    /** Tools the model called, e.g. find_available_rooms - shows the answer was looked up, not guessed. */
    List<String> toolsUsed;
}
