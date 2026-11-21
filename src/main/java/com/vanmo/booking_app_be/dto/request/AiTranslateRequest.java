package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Which language a branch's page should be shown in. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiTranslateRequest {
    /** A language code the app offers: en, ko, ja, zh, fr, ru. */
    @NotBlank
    @Size(max = 8)
    String lang;
}
