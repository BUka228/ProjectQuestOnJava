package com.example.projectquestonjava.approach.calendar.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.domain.repository.TaskTagRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarParamsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class UpdateCalendarTaskUseCase {
    private static final String TAG = "UpdateCalendarTaskUseCase";

    private final TaskRepository taskRepository;
    private final CalendarParamsRepository calendarParamsRepository;
    private final TaskTagRepository taskTagRepository;
    private final UnitOfWork unitOfWork;
    private final UserSessionManager userSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public UpdateCalendarTaskUseCase(
            TaskRepository taskRepository,
            CalendarParamsRepository calendarParamsRepository,
            TaskTagRepository taskTagRepository,
            UnitOfWork unitOfWork,
            UserSessionManager userSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskRepository = taskRepository;
        this.calendarParamsRepository = calendarParamsRepository;
        this.taskTagRepository = taskTagRepository;
        this.unitOfWork = unitOfWork;
        this.userSessionManager = userSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(TaskInput taskInput) {
        logger.debug(TAG, "execute started for task update: " + (taskInput.getId() != null ? taskInput.getId() : "NEW_ID_ERROR") + " - " + taskInput.getTitle());

        return Futures.submit(() -> {
            try {
                Long taskId = taskInput.getId();
                if (taskId == null) {
                    String errorMsg = "Task ID is missing in TaskInput for update.";
                    logger.error(TAG, errorMsg);
                    throw new IllegalArgumentException(errorMsg);
                }

                int userId = userSessionManager.getUserIdSync();
                if (userId == UserSessionManager.NO_USER_ID) {
                    String errorMsg = "Cannot update task " + taskId + ", user not logged in.";
                    logger.error(TAG, errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                logger.debug(TAG, "Attempting to update task " + taskId + " for userId " + userId);

                unitOfWork.withTransaction((Callable<Void>) () -> {
                    logger.debug(TAG, "Update transaction started for task: " + taskId);

                    // 1. Получить и проверить текущую задачу
                    Task currentTask = taskRepository.getTaskByIdSync(taskId); // Используем getTaskByIdSync(taskId), userId проверится ниже
                    if (currentTask == null) {
                        throw new NoSuchElementException("Task " + taskId + " not found.");
                    }
                    if (currentTask.getUserId() != userId) {
                        throw new SecurityException("Task " + taskId + " does not belong to current user " + userId + " (owner: " + currentTask.getUserId() + ").");
                    }

                    // 2. Обновить Task
                    LocalDateTime dueDateUtc = dateTimeUtils.localToUtcLocalDateTime(taskInput.getDueDate());
                    Task taskToUpdateInDb = new Task(
                            currentTask.getId(), currentTask.getUserId(), currentTask.getWorkspaceId(),
                            taskInput.getTitle().trim(), taskInput.getDescription().trim(),
                            dueDateUtc, currentTask.getStatus(), currentTask.getCreatedAt(),
                            dateTimeUtils.currentUtcDateTime()
                    );
                    logger.debug(TAG, "Updating Task entity in DB for ID: " + taskId);
                    taskRepository.updateTaskSync(taskToUpdateInDb);
                    logger.debug(TAG, "Task entity " + taskId + " updated. UpdatedAt: " + taskToUpdateInDb.getUpdatedAt());

                    // 3. Обновить CalendarParams
                    CalendarParams currentParams = calendarParamsRepository.getParamsByTaskIdSync(taskId);
                    if (currentParams == null) {
                        logger.warn(TAG, "CalendarParams not found for task " + taskId + " during update. Creating new one.");
                        // Создаем новые параметры, если они отсутствуют
                        CalendarParams newParams = new CalendarParams(taskId, UUID.randomUUID().toString(), false, taskInput.getRecurrenceRule());
                        calendarParamsRepository.insertParamsSync(newParams);
                        logger.info(TAG, "Created and inserted new CalendarParams for task " + taskId);
                    } else if (!Objects.equals(currentParams.getRecurrenceRule(), taskInput.getRecurrenceRule()) ||
                            currentParams.isAllDay() /* Пример, если isAllDay тоже можно менять */) {

                        CalendarParams updatedParams = new CalendarParams(
                                currentParams.getTaskId(), currentParams.getEventId(),
                                currentParams.isAllDay(), // Пока оставляем старое значение
                                taskInput.getRecurrenceRule()
                        );
                        logger.debug(TAG, "CalendarParams changed. Updating in DB for task ID: " + taskId);
                        calendarParamsRepository.updateParamsSync(updatedParams);
                    } else {
                        logger.debug(TAG, "CalendarParams unchanged for task ID: " + taskId);
                    }

                    // 4. Обновить Теги
                    logger.debug(TAG, "Deleting old tag associations for task " + taskId + " (SYNC).");
                    taskTagRepository.deleteTaskTagsByTaskIdSync(taskId);

                    if (taskInput.getSelectedTags() != null && !taskInput.getSelectedTags().isEmpty()) {
                        List<TaskTagCrossRef> newCrossRefs = taskInput.getSelectedTags().stream()
                                .map(tag -> new TaskTagCrossRef(taskId, tag.getId()))
                                .collect(Collectors.toList());
                        logger.debug(TAG, "Inserting " + newCrossRefs.size() + " new tag cross refs for task ID: " + taskId + " (SYNC).");
                        taskTagRepository.insertAllTaskTagSync(newCrossRefs);
                    } else {
                        logger.debug(TAG, "No new tags to insert for task ID: " + taskId);
                    }

                    logger.info(TAG, "Task " + taskId + " updated successfully within transaction.");
                    return null;
                });
                return null;
            } catch (Exception e) {
                logger.error(TAG, "Failed to update task " + (taskInput.getId() != null ? taskInput.getId() : "UNKNOWN_ID"), e);
                if (e.getCause() != null) logger.error(TAG, "Cause: ", e.getCause());
                throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException("Failed to update task", e);
            }
        }, ioExecutor);
    }
}