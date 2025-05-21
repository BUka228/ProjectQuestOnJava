package com.example.projectquestonjava.core.priority_strategy;

import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import java.time.LocalDateTime;
import javax.inject.Inject;

public class OverdueCriticalStrategy implements PriorityStrategy {
    private final DateTimeUtils dateTimeUtils;

    @Inject
    public OverdueCriticalStrategy(DateTimeUtils dateTimeUtils) {
        this.dateTimeUtils = dateTimeUtils;
    }

    @Override
    public boolean canHandle(LocalDateTime dueDate, TaskStatus status) {
        long minutesUntilDue = dateTimeUtils.calculateDurationUntilDue(dueDate);
        return status != TaskStatus.DONE && minutesUntilDue < 0;
    }

    @Override
    public Priority getPriority() {
        return Priority.CRITICAL;
    }
}