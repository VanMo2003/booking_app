package com.vanmo.booking_app_be.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vanmo.booking_app_be.dto.request.ItineraryStepRequest;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * A draft of a tour's long fields for the branch to edit and save - nothing is written to the tour here. The fields
 * match the tour form one for one, so the app can drop them straight into it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiTourDraftResponse {

    String description;

    /** Comma-separated, the way the form stores it. */
    String includes;

    String cancellationPolicy;

    List<ItineraryStepRequest> itinerary;
}
