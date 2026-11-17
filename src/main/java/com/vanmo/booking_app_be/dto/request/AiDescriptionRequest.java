package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Which row the assistant should describe, plus whatever the manager wants mentioned. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiDescriptionRequest {
    /** ROOM, DISH or SERVICE. */
    @NotBlank
    String kind;

    @NotNull
    Long id;

    /** Optional hint from the manager, e.g. "phòng mới sửa, nhìn ra hồ". */
    @Size(max = 300)
    String notes;
}
