package com.example.projectquestonjava.core.priority_strategy;

import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import java.time.LocalDateTime;
import javax.inject.Inject;

public class LowPriorityStrategy implements PriorityStrategy {
    @Inject
    public LowPriorityStrategy() {}

    @Override
    public boolean canHandle(LocalDateTime dueDate, TaskStatus status) {
        return status != TaskStatus.DONE;
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }
}