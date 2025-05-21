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

    // Вместо Result<Long> используем ListenableFuture<Long>, который может завершиться с исключением
    public ListenableFuture<Long> execute(TaskInput taskInput) {
        // Выполняем всю логику на ioExecutor
        return Futures.submit(() -> {
            try {
                int userId = userSessionManager.getUserIdSync(); // Блокирующий вызов, но мы уже на ioExecutor
                long workspaceId = workspaceSessionManager.getWorkspaceIdSync(); // Аналогично

                if (userId == UserSessionManager.NO_USER_ID || workspaceId == 0L) {
                    logger.error(TAG, "User or Workspace not set for task creation.");
                    throw new IllegalStateException("User or Workspace not set");
                }
                logger.debug(TAG, "Creating task '" + taskInput.getTitle() + "' for userId=" + userId + ", workspaceId=" + workspaceId);

                // Выполняем все операции с БД внутри транзакции
                return unitOfWork.withTransaction(() -> {
                    // 1. Task через TaskRepository
                    Task taskToInsert = taskFactory.create(taskInput, workspaceId, userId);
                    // .get() на ListenableFuture блокирует поток, что нормально внутри withTransaction на ioExecutor
                    Long taskId = taskRepository.insertTask(taskToInsert).get();
                    logger.debug(TAG, "Task inserted with id=" + taskId);

                    // 2. Обновление Global Stats
                    // .get() для ListenableFuture<Void> просто ждет завершения или бросает исключение
                    globalStatisticsRepository.incrementTotalTasks().get();
                    logger.debug(TAG, "Incremented global total tasks.");

                    // 3. Params через CalendarParamsRepository
                    CalendarParams calendarParams = calendarParamsFactory.create(taskId, taskInput.getRecurrenceRule());
                    calendarParamsRepository.insertParams(calendarParams).get();
                    logger.debug(TAG, "CalendarParams inserted for taskId=" + taskId);

                    // 4. Теги через TaskTagRepository
                    if (taskInput.getSelectedTags() != null && !taskInput.getSelectedTags().isEmpty()) {
                        List<TaskTagCrossRef> crossRefs = taskInput.getSelectedTags().stream()
                                .map(tag -> new TaskTagCrossRef(taskId, tag.getId()))
                                .collect(Collectors.toList());
                        taskTagRepository.insertAllTaskTag(crossRefs).get();
                        logger.debug(TAG, "Inserted " + crossRefs.size() + " tag cross refs for taskId=" + taskId);
                    }
                    return taskId; // Возвращаем ID созданной задачи
                });
            } catch (Exception e) {
                logger.error(TAG, "Failed to create task: " + taskInput.getTitle(), e);
                throw e; // Пробрасываем исключение, чтобы ListenableFuture завершился с ошибкой
            }
        }, ioExecutor);
    }
}