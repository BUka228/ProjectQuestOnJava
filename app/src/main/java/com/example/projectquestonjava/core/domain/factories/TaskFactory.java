package com.example.projectquestonjava.core.domain.factories;

import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.core.data.model.core.Task;


public interface TaskFactory {
    Task create(TaskInput taskInput, long workspaceId, int userId);
}