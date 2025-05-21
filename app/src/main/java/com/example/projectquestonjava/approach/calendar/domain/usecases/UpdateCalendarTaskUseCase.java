package com.example.projectquestonjava.approach.calendar.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.data.model.core.Tag;
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
        return Futures.submit(() -> {
            try {
                Long taskId = taskInput.getId();
                if (taskId == null) {
                    logger.error(TAG, "Task ID is missing in TaskInput for update.");
                    throw new IllegalArgumentException("Task ID is required for updates.");
                }

                int userId = userSessionManager.getUserIdSync();
                if (userId == UserSessionManager.NO_USER_ID) {
                    logger.error(TAG, "Cannot update task " + taskId + ", user not logged in.");
                    throw new IllegalStateException("User not logged in");
                }
                logger.debug(TAG, "Attempting to update task " + taskId + " for userId " + userId);

                unitOfWork.withTransaction(() -> {
                    // --- 1. Обновление Task ---
                    Task currentTask = taskRepository.getTaskById(taskId).get(); // Блокирующий вызов внутри транзакции
                    if (currentTask == null || currentTask.getUserId() != userId) {
                        throw new NoSuchElementException("Task " + taskId + " not found or access denied for user " + userId + ".");
                    }

                    LocalDateTime dueDateUtc = dateTimeUtils.localToUtcLocalDateTime(taskInput.getDueDate());
                    logger.debug(TAG, "Converted local dueDate " + taskInput.getDueDate() + " to UTC dueDate " + dueDateUtc + " for saving.");

                    // Создаем новый объект Task для обновления, используя сеттеры или новый конструктор
                    Task updatedTask = new Task(
                            currentTask.getId(),
                            currentTask.getUserId(),
                            currentTask.getWorkspaceId(),
                            taskInput.getTitle().trim(),
                            taskInput.getDescription().trim(),
                            dueDateUtc,
                            currentTask.getStatus(), // Статус не меняем здесь
                            currentTask.getCreatedAt(), // createdAt не меняем
                            dateTimeUtils.currentUtcDateTime() // updatedAt всегда текущее UTC
                    );
                    taskRepository.updateTask(updatedTask).get();
                    logger.debug(TAG, "Task entity " + taskId + " updated. UpdatedAt set to UTC: " + updatedTask.getUpdatedAt());

                    // --- 2. Обновление CalendarParams ---
                    CalendarParams currentParams = calendarParamsRepository.getParamsByTaskId(taskId).get();
                    if (currentParams == null) {
                        throw new IllegalStateException("CalendarParams not found for task " + taskId + " during update.");
                    }

                    // Создаем новый CalendarParams если нужно обновить
                    // В Java нет простого copy, создаем новый объект
                    if (!Objects.equals(currentParams.getRecurrenceRule(), taskInput.getRecurrenceRule())) {
                        CalendarParams updatedParams = new CalendarParams(
                                currentParams.getTaskId(),
                                currentParams.getEventId(),
                                currentParams.isAllDay(),
                                taskInput.getRecurrenceRule()
                        );
                        calendarParamsRepository.updateParams(updatedParams).get();
                        logger.debug(TAG, "CalendarParams for task " + taskId + " updated.");
                    } else {
                        logger.debug(TAG, "CalendarParams for task " + taskId + " had no changes.");
                    }


                    // --- 3. Обновление Тегов ---
                    logger.debug(TAG, "Deleting old tag associations for task " + taskId + ".");
                    taskTagRepository.deleteTaskTagsByTaskId(taskId).get();

                    if (taskInput.getSelectedTags() != null && !taskInput.getSelectedTags().isEmpty()) {
                        List<TaskTagCrossRef> newCrossRefs = taskInput.getSelectedTags().stream()
                                .map(tag -> new TaskTagCrossRef(taskId, tag.getId()))
                                .collect(Collectors.toList());
                        taskTagRepository.insertAllTaskTag(newCrossRefs).get();
                        logger.debug(TAG, "Inserted " + newCrossRefs.size() + " new tag cross refs for task " + taskId);
                    } else {
                        logger.debug(TAG, "No tags selected for task " + taskId + ".");
                    }

                    logger.info(TAG, "Task " + taskId + " updated successfully within transaction.");
                    return null; // Callable должен что-то вернуть
                });
            } catch (Exception e) {
                logger.error(TAG, "Failed to update task " + (taskInput.getId() != null ? taskInput.getId() : "UNKNOWN"), e);
                throw new RuntimeException("Failed to update task", e);
            }
            return null; // Для Futures.submit
        }, ioExecutor);
    }
}