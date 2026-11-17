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

/**
 * A draft description for one room, dish or service. Nothing is saved: the manager edits it and sends the
 * ordinary update request for that row, which is the only thing that writes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiDescriptionResponse {

    /** ROOM, DISH or SERVICE. */
    String kind;

    /** The row the draft is for. */
    Long id;

    String description;

    /** A few short selling points, from the row's own facts. */
    List<String> highlights;

    /** What the row says today, so the app can warn before replacing it. */
    String currentDescription;

    List<String> notes;
}
