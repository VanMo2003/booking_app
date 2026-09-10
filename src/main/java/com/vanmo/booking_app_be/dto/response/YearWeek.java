package com.vanmo.booking_app_be.dto.response;

import java.time.LocalDate;
import java.time.temporal.WeekFields;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YearWeek {

    private final int year;
    private final int week;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public static YearWeek from(LocalDate date) {
        WeekFields wf = WeekFields.ISO;
        int week = date.get(wf.weekOfWeekBasedYear());
        int year = date.get(wf.weekBasedYear());

        LocalDate start = date.with(wf.weekOfWeekBasedYear(), week).with(wf.dayOfWeek(), 1);

        LocalDate end = start.plusDays(6);

        return new YearWeek(year, week, start, end);
    }
}
