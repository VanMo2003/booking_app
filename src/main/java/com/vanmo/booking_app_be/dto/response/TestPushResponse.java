package com.vanmo.booking_app_be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** What a test push will do: nothing when FCM is off on the server or the account has no registered device. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestPushResponse {
    boolean pushEnabled;
    int devices;
    int delaySeconds;
}
