package com.example.projectquestonjava.approach.calendar.domain.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarMonthData {
    private List<CalendarTaskSummary> tasks = Collections.emptyList();
    private Map<LocalDate, Integer> dailyTaskCounts = Collections.emptyMap();

    public static final CalendarMonthData EMPTY = new CalendarMonthData(Collections.emptyList(), Collections.emptyMap());
}