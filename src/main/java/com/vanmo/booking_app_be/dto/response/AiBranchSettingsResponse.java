package com.vanmo.booking_app_be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiBranchSettingsResponse {
    Long hotelId;
    boolean autoReply;

    /** False when the server has no Claude API key - the switch is then shown but has no effect. */
    boolean aiEnabled;

    long autoReplyDelaySeconds;
}
