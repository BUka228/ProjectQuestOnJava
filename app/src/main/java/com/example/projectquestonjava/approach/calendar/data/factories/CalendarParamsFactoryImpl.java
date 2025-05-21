package com.example.projectquestonjava.approach.calendar.data.factories;

import androidx.annotation.Nullable;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.example.projectquestonjava.approach.calendar.domain.factories.CalendarParamsFactory;

import java.util.UUID;
import javax.inject.Inject;

public class CalendarParamsFactoryImpl implements CalendarParamsFactory {

    @Inject
    public CalendarParamsFactoryImpl() {
    }

    @Override
    public CalendarParams create(long taskId, @Nullable String recurrenceRule) {
        return new CalendarParams(
                taskId,
                UUID.randomUUID().toString(),
                false,
                recurrenceRule
        );
    }
}
