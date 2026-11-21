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
 * A draft answer to one guest review. Nothing is published: the manager reads it, edits it and posts it with
 * {@code PUT /feedbacks/{id}/reply}, which is the only thing that writes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiReviewReplyResponse {

    Long feedbackId;

    Integer rating;

    /** The draft itself, for the manager to edit. */
    String draft;

    /** What the guest actually complained about or praised, as the assistant read it - one line each. */
    List<String> points;

    /** Set when the review already has an answer, so the app can warn before replacing it. */
    String currentReply;

    List<String> notes;
}
