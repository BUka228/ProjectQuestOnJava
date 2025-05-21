package com.example.projectquestonjava.core.domain.repository;

import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import java.time.LocalDateTime;

public interface PriorityResolver {
    Priority resolve(LocalDateTime dueDate, TaskStatus status);
}