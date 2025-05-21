package com.example.projectquestonjava.core.data.factories;

import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.domain.factories.TaskFactory;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskFactoryImpl implements TaskFactory {

    private final DateTimeUtils dateTimeUtils;

    @Inject
    public TaskFactoryImpl(DateTimeUtils dateTimeUtils) {
        this.dateTimeUtils = dateTimeUtils;
    }

    @Override
    public Task create(TaskInput taskInput, long workspaceId, int userId) {
        LocalDateTime nowUtc = dateTimeUtils.currentUtcDateTime();
        boolean isNewTask = taskInput.getId() == null || taskInput.getId() == 0;

        LocalDateTime dueDateUtc = dateTimeUtils.localToUtcLocalDateTime(taskInput.getDueDate());

        return new Task(
                taskInput.getId() != null ? taskInput.getId() : 0L,
                userId,
                workspaceId,
                taskInput.getTitle().trim(),
                taskInput.getDescription().trim(),
                dueDateUtc,
                TaskStatus.TODO,
                nowUtc,
                nowUtc
        );
    }
}