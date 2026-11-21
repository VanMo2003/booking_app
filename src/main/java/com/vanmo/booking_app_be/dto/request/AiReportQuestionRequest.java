package com.vanmo.booking_app_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/** A question about the numbers, asked by a branch manager (hotelId) or a chain owner (hotelChainId). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiReportQuestionRequest {

    /** "Doanh thu tháng này so với tháng trước thế nào?", "Tour nào bán tốt nhất?" */
    @NotBlank
    @Size(max = 300)
    String query;

    /** One of the two: the branch to look at, or the chain. */
    Long hotelId;

    Long hotelChainId;
}
