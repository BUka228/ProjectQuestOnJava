package com.example.projectquestonjava.approach.calendar.domain.model;

import androidx.annotation.Nullable;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDashboardData {
    private List<CalendarTaskSummary> tasks = Collections.emptyList();
    @Nullable
    private Gamification gamification;

    public static final CalendarDashboardData EMPTY = new CalendarDashboardData(Collections.emptyList(), null);
}