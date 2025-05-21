package com.example.projectquestonjava.approach.calendar.domain.factories;


import androidx.annotation.Nullable;

import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;


public interface CalendarParamsFactory {
    CalendarParams create(long taskId, @Nullable String recurrenceRule);
}
