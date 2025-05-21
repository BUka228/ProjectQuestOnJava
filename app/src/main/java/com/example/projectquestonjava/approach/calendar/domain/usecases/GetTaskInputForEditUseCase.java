package com.example.projectquestonjava.approach.calendar.domain.usecases;

import com.example.projectquestonjava.core.data.model.relations.TaskWithTags;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarParamsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class GetTaskInputForEditUseCase {
    private static final String TAG = "GetTaskInputForEditUseCase";

    private final TaskRepository taskRepository;
    private final CalendarParamsRepository calendarParamsRepository;
    private final UserSessionManager userSessionManager;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public GetTaskInputForEditUseCase(
            TaskRepository taskRepository,
            CalendarParamsRepository calendarParamsRepository,
            UserSessionManager userSessionManager,
            WorkspaceSessionManager workspaceSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskRepository = taskRepository;
        this.calendarParamsRepository = calendarParamsRepository;
        this.userSessionManager = userSessionManager;
        this.workspaceSessionManager = workspaceSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    // Вместо Result<TaskInput> используем ListenableFuture<TaskInput>
    public ListenableFuture<TaskInput> execute(long taskId) throws ExecutionException, InterruptedException {
        return Futures.submit(() -> {
            try {
                int userId = userSessionManager.getUserIdSync();
                if (userId == UserSessionManager.NO_USER_ID) {
                    throw new IllegalStateException("User not logged in");
                }
                long workspaceId = workspaceSessionManager.getWorkspaceIdSync();
                if (workspaceId == 0L) {
                    throw new IllegalStateException("No active workspace set");
                }
                logger.debug(TAG, "Fetching task " + taskId + " for edit in workspace " + workspaceId + " (user " + userId + ")");

                // Используем Futures.allAsList для параллельного выполнения, если это возможно
                // и если TaskRepository.getTaskWithTagsById и CalendarParamsRepository.getParamsByTaskId
                // действительно асинхронны (возвращают ListenableFuture).
                // Если они блокирующие, то вызов в submit уже достаточен.
                // Для ListenableFuture, возвращаемых DAO, они уже асинхронны.

                ListenableFuture<TaskWithTags> taskWithTagsFuture = taskRepository.getTaskWithTagsById(taskId); // userId неявно в TaskRepository
                ListenableFuture<CalendarParams> paramsFuture = calendarParamsRepository.getParamsByTaskId(taskId);

                // Объединяем результаты
                ListenableFuture<List<Object>> combinedFuture = Futures.allAsList(taskWithTagsFuture, paramsFuture);

                return Futures.transform(combinedFuture, results -> {
                    TaskWithTags taskWithTags = (TaskWithTags) results.get(0);
                    CalendarParams params = (CalendarParams) results.get(1);

                    if (taskWithTags == null) {
                        throw new NoSuchElementException("Task with id " + taskId + " not found or access denied");
                    }
                    if (params == null) {
                        throw new NoSuchElementException("CalendarParams not found for task " + taskId);
                    }

                    if (taskWithTags.getTask().getWorkspaceId() != workspaceId) {
                        throw new SecurityException("Task " + taskId + " does not belong to workspace " + workspaceId);
                    }

                    TaskInput taskInput = mapToTaskInput(taskWithTags, params, dateTimeUtils);
                    logger.info(TAG, "Task " + taskId + " loaded for edit.");
                    return taskInput;
                }, MoreExecutors.directExecutor()); // Преобразование можно выполнить в том же потоке, если futures уже на ioExecutor

            } catch (Exception e) {
                logger.error(TAG, "Failed to load task " + taskId + " for edit", e);
                // Пробрасываем для ListenableFuture
                // Для возврата null или специфичного значения ошибки, Futures.catchingAsync можно использовать
                throw new RuntimeException("Failed to load task for edit", e);
            }
        }, ioExecutor).get();
    }
    public ListenableFuture<ListenableFuture<TaskInput>> executeAsync(long taskId) {
        return Futures.submit(() -> {
            try {
                int userId = userSessionManager.getUserIdSync();
                if (userId == UserSessionManager.NO_USER_ID) {
                    throw new IllegalStateException("User not logged in");
                }
                long workspaceId = workspaceSessionManager.getWorkspaceIdSync();
                if (workspaceId == 0L) {
                    throw new IllegalStateException("No active workspace set");
                }
                logger.debug(TAG, "Fetching task " + taskId + " for edit in workspace " + workspaceId + " (user " + userId + ")");

                ListenableFuture<TaskWithTags> taskWithTagsFuture = taskRepository.getTaskWithTagsById(taskId);
                ListenableFuture<CalendarParams> paramsFuture = calendarParamsRepository.getParamsByTaskId(taskId);

                // Используем transformAsync для последовательного выполнения с проверками
                return Futures.transformAsync(taskWithTagsFuture, taskWithTags -> {
                    if (taskWithTags == null) {
                        throw new NoSuchElementException("Task with id " + taskId + " not found or access denied");
                    }
                    if (taskWithTags.getTask().getWorkspaceId() != workspaceId) {
                        throw new SecurityException("Task " + taskId + " does not belong to workspace " + workspaceId);
                    }

                    return Futures.transform(paramsFuture, params -> {
                        if (params == null) {
                            throw new NoSuchElementException("CalendarParams not found for task " + taskId);
                        }
                        TaskInput taskInput = mapToTaskInput(taskWithTags, params, dateTimeUtils);
                        logger.info(TAG, "Task " + taskId + " loaded for edit.");
                        return taskInput;
                    }, MoreExecutors.directExecutor()); // Преобразование params
                }, ioExecutor); // Преобразование taskWithTagsFuture

            } catch (Exception e) {
                logger.error(TAG, "Failed to load task " + taskId + " for edit", e);
                throw new RuntimeException("Failed to load task for edit", e); // Обертываем для ListenableFuture
            }
        }, ioExecutor);
    }


    private TaskInput mapToTaskInput(
            TaskWithTags taskWithTags,
            CalendarParams params,
            DateTimeUtils dateTimeUtils
    ) {
        return new TaskInput(
                taskWithTags.getTask().getId(),
                taskWithTags.getTask().getTitle(),
                taskWithTags.getTask().getDescription(),
                dateTimeUtils.utcToLocalLocalDateTime(taskWithTags.getTask().getDueDate()), // Конвертируем в локальное
                params.getRecurrenceRule(),
                new HashSet<>(taskWithTags.getTags()) // Преобразуем List в Set
        );
    }
}