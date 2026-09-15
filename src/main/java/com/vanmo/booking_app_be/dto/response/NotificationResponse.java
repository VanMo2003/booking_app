package com.vanmo.booking_app_be.dto.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.entity.enums.NotificationType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    Long id;
    NotificationType type;
    String title;
    String body;
    String link;
    Long referenceId;
    boolean read;
    Date onCreate;
}
