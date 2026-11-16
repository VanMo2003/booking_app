package com.vanmo.booking_app_be.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.vanmo.booking_app_be.entity.enums.TravelTheme;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/** What the branch already typed into the tour form; the assistant writes the long fields from it. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiTourDraftRequest {

    @NotNull
    Long hotelId;

    @NotBlank
    @Size(max = 200)
    String name;

    @Size(max = 200)
    String destination;

    /** "4 giờ", "2 ngày 1 đêm". */
    @Size(max = 100)
    String duration;

    /** "8:00 hằng ngày". */
    @Size(max = 100)
    String departure;

    Set<TravelTheme> themes;

    /** Anything else the branch wants mentioned: "có xe limousine", "phù hợp gia đình có trẻ nhỏ". */
    @Size(max = 300)
    String notes;
}
