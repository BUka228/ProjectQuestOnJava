package com.example.projectquestonjava.core.priority_strategy;

import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.domain.model.PriorityThresholds;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import java.time.LocalDateTime;
import javax.inject.Inject;

public class MediumPriorityStrategy implements PriorityStrategy {
    private final DateTimeUtils dateTimeUtils;
    private final PriorityThresholds thresholds;

    @Inject
    public MediumPriorityStrategy(DateTimeUtils dateTimeUtils, PriorityThresholds thresholds) {
        this.dateTimeUtils = dateTimeUtils;
        this.thresholds = thresholds;
    }

    @Override
    public boolean canHandle(LocalDateTime dueDate, TaskStatus status) {
        return status != TaskStatus.DONE &&
                dateTimeUtils.calculateDurationUntilDue(dueDate) <= thresholds.getMedium();
    }

    @Override
    public Priority getPriority() {
        return Priority.MEDIUM;
    }
}