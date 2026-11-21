package com.vanmo.booking_app_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** One stop of a tour's programme, shown as a timeline: "Ngày 1 · 08:00", "Đón khách tại khách sạn", details. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItineraryStep {
    /** Free text: "08:00", "Ngày 2 · Sáng". */
    @Column(name = "step_time", length = 60)
    String time;

    @Column(name = "step_title", length = 255, nullable = false)
    String title;

    @Column(name = "step_description", length = 2000)
    String description;
}
