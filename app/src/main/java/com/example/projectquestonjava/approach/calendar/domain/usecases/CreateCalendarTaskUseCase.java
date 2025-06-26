package com.example.projectquestonjava.approach.calendar.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.factories.TaskFactory;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.domain.repository.TaskTagRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.example.projectquestonjava.approach.calendar.domain.factories.CalendarParamsFactory;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarParamsRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.GlobalStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class CreateCalendarTaskUseCase {
    private static final String TAG = "CreateCalendarTaskUseCase";

    private final TaskRepository taskRepository;
    private final CalendarParamsRepository calendarParamsRepository;
    private final TaskTagRepository taskTagRepository;
    private final GlobalStatisticsRepository globalStatisticsRepository;
    private final TaskFactory taskFactory;
    private final CalendarParamsFactory calendarParamsFactory;
    private final UnitOfWork unitOfWork;
    private final UserSessionManager userSessionManager;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public CreateCalendarTaskUseCase(
            TaskRepository taskRepository,
            CalendarParamsRepository calendarParamsRepository,
            TaskTagRepository taskTagRepository,
            GlobalStatisticsRepository globalStatisticsRepository,
            TaskFactory taskFactory,
            CalendarParamsFactory calendarParamsFactory,
            UnitOfWork unitOfWork,
            UserSessionManager userSessionManager,
            WorkspaceSessionManager workspaceSessionManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskRepository = taskRepository;
        this.calendarParamsRepository = calendarParamsRepository;
        this.taskTagRepository = taskTagRepository;
        this.globalStatisticsRepository = globalStatisticsRepository;
        this.taskFactory = taskFactory;
        this.calendarParamsFactory = calendarParamsFactory;
        this.unitOfWork = unitOfWork;
        this.userSessionManager = userSessionManager;
        this.workspaceSessionManager = workspaceSessionManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Long> execute(TaskInput taskInput) {
        return Futures.submit(() -> {
            logger.debug(TAG, "execute started for task: " + taskInput.getTitle());
            try {
                int userId = userSessionManager.getUserIdSync();
                long workspaceId = workspaceSessionManager.getWorkspaceIdSync();

                logger.info(TAG, "Attempting task creation with UserId: " + userId + ", WorkspaceId: " + workspaceId);

                if (userId == UserSessionManager.NO_USER_ID || workspaceId == 0L) {
                    String errorMsg = "User or Workspace not set for task creation. UserId: " + userId + ", WorkspaceId: " + workspaceId;
                    logger.error(TAG, errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                logger.debug(TAG, "Creating task '" + taskInput.getTitle() + "' for userId=" + userId + ", workspaceId=" + workspaceId);


                return unitOfWork.withTransaction((Callable<Long>) () -> {
                    logger.debug(TAG, "Transaction started.");
                    // 1. Task
                    Task taskToInsert = taskFactory.create(taskInput, workspaceId, userId);
                    logger.debug(TAG, "Task object created. Attempting insertTaskSync...");
                    long taskId = taskRepository.insertTaskSync(taskToInsert);
                    logger.info(TAG, "Task inserted (SYNC) with id=" + taskId);


                    // 2. Global Stats
                    logger.debug(TAG, "Attempting to increment global total tasks (SYNC)...");
                    globalStatisticsRepository.incrementTotalTasksSync();
                    logger.info(TAG, "Incremented global total tasks (SYNC).");


                    // 3. Calendar Params
                    CalendarParams calendarParams = calendarParamsFactory.create(taskId, taskInput.getRecurrenceRule());
                    logger.debug(TAG, "CalendarParams created. Attempting insertParamsSync for taskId=" + taskId);
                    calendarParamsRepository.insertParamsSync(calendarParams);
                    logger.info(TAG, "CalendarParams inserted (SYNC) for taskId=" + taskId);


                    // 4. Tags
                    if (taskInput.getSelectedTags() != null && !taskInput.getSelectedTags().isEmpty()) {
                        List<TaskTagCrossRef> crossRefs = taskInput.getSelectedTags().stream()
                                .map(tag -> new TaskTagCrossRef(taskId, tag.getId()))
                                .collect(Collectors.toList());
                        logger.debug(TAG, "Tag cross refs created ("+crossRefs.size()+"). Attempting insertAllTaskTagSync for taskId=" + taskId);
                        taskTagRepository.insertAllTaskTagSync(crossRefs);
                        logger.info(TAG, "Inserted " + crossRefs.size() + " tag cross refs (SYNC) for taskId=" + taskId);
                    } else {
                        logger.debug(TAG, "No tags selected for task " + taskId);
                    }
                    logger.info(TAG, "Transaction completed successfully for task " + taskId);
                    return taskId;
                });
            } catch (Exception e) {
                logger.error(TAG, "Failed to create task in CreateCalendarTaskUseCase: " + taskInput.getTitle() + ". Exception type: " + e.getClass().getName(), e);
                if (e.getCause() != null) {
                    logger.error(TAG, "Cause: " + e.getCause().getMessage(), e.getCause());
                }
                throw e;
            }
        }, ioExecutor);
    }
}