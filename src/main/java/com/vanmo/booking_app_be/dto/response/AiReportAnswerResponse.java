package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;
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
 * An answer about the numbers, written from the report figures the backend read for the period in question. The
 * figures travel with it ({@link #summary}), so the app can show the real KPIs next to the text and the reader can
 * check every claim.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiReportAnswerResponse {

    String question;

    /** The period the backend actually read, after checking what the model understood. */
    LocalDate from;

    LocalDate to;

    /** "branch" or "chain". */
    String scope;

    /** A few sentences answering the question, in Vietnamese. */
    String answer;

    /** The numbers worth noticing, one line each. */
    List<String> highlights;

    /** What the owner or manager could do next, one line each. */
    List<String> suggestions;

    /** The KPIs the answer was written from. */
    RevenueSummaryResponse summary;

    /** Tours of the period, best first - empty when none were completed. */
    List<RevenueByTourDTO> tours;

    /** Chain scope only: how the branches compare over the period. */
    List<BranchPerformanceDTO> branches;

    List<String> notes;
}
