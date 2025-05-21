package com.example.projectquestonjava.core.priority_strategy;

import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import java.time.LocalDateTime;

public interface PriorityStrategy {
    boolean canHandle(LocalDateTime dueDate, TaskStatus status);
    Priority getPriority();
}