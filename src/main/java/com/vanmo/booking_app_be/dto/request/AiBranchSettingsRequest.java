package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiBranchSettingsRequest {
    /** Let the chat assistant answer guests nobody on the team answered in time. */
    @NotNull
    Boolean autoReply;
}
