package com.example.projectquestonjava.feature.pomodoro.domain.usecases;

import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.data.model.relations.TaskWithTags;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.domain.usecases.ProcessTaskCompletionUseCase;
import com.example.projectquestonjava.feature.pomodoro.domain.model.InterruptedPhaseInfo;
import com.example.projectquestonjava.feature.statistics.domain.repository.TaskStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import androidx.annotation.Nullable;

public class ForceCompleteTaskWithPomodoroUseCase {
    private static final String TAG = "ForceCompleteTaskWithPomodoroUC";

    private final TaskRepository taskRepository;
    private final TaskStatisticsRepository taskStatisticsRepository;
    private final CompletePomodoroSessionUseCase completePomodoroSessionUseCase;
    private final ProcessTaskCompletionUseCase processTaskCompletionUseCase;
    private final UnitOfWork unitOfWork;
    private final DateTimeUtils dateTimeUtils;
    private final UserSessionManager userSessionManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public ForceCompleteTaskWithPomodoroUseCase(
            TaskRepository taskRepository, TaskStatisticsRepository taskStatisticsRepository,
            CompletePomodoroSessionUseCase completePomodoroSessionUseCase,
            ProcessTaskCompletionUseCase processTaskCompletionUseCase, UnitOfWork unitOfWork,
            DateTimeUtils dateTimeUtils, UserSessionManager userSessionManager,
            @IODispatcher Executor ioExecutor, Logger logger) {
        this.taskRepository = taskRepository;
        this.taskStatisticsRepository = taskStatisticsRepository;
        this.completePomodoroSessionUseCase = completePomodoroSessionUseCase;
        this.processTaskCompletionUseCase = processTaskCompletionUseCase;
        this.unitOfWork = unitOfWork;
        this.dateTimeUtils = dateTimeUtils;
        this.userSessionManager = userSessionManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(long taskId, @Nullable InterruptedPhaseInfo interruptedPhaseInfo) {
        return Futures.submit(() -> {
            logger.info(TAG, "Forcing completion for task " + taskId + ". Interrupted phase: " + (interruptedPhaseInfo != null));
            try {
                int userId = userSessionManager.getUserIdSync();
                if (userId == UserSessionManager.NO_USER_ID) {
                    throw new IllegalStateException("User not logged in for ForceCompleteTask");
                }

                unitOfWork.withTransaction((Callable<Void>) () -> {
                    LocalDateTime completionTimeUtc = dateTimeUtils.currentUtcDateTime();

                    if (interruptedPhaseInfo != null) {
                        long actualDurationSeconds = Duration.between(interruptedPhaseInfo.getStartTime(), completionTimeUtc).getSeconds();
                        actualDurationSeconds = Math.max(0, actualDurationSeconds);

                        logger.debug(TAG, "Processing interrupted Pomodoro phase " + interruptedPhaseInfo.getDbSessionId() +
                                " for task " + taskId + ". Type: " + interruptedPhaseInfo.getType() +
                                ", ActualDuration: " + actualDurationSeconds + "s, Interruptions: " + interruptedPhaseInfo.getInterruptions());
                        // Вызываем execute и дожидаемся, т.к. мы внутри транзакции и на ioExecutor
                        completePomodoroSessionUseCase.execute(
                                interruptedPhaseInfo.getDbSessionId(),
                                taskId,
                                interruptedPhaseInfo.getType(),
                                (int) actualDurationSeconds,
                                interruptedPhaseInfo.getInterruptions()
                        ).get(); // Дожидаемся завершения
                        logger.debug(TAG, "Interrupted Pomodoro phase " + interruptedPhaseInfo.getDbSessionId() + " processed.");
                    }

                    TaskWithTags taskWithTags = taskRepository.getTaskWithTagsByIdSync(taskId, userId);
                    if (taskWithTags == null) {
                        throw new NoSuchElementException("Task " + taskId + " not found for completion.");
                    }
                    List<Tag> tags = taskWithTags.getTags() != null ? taskWithTags.getTags() : Collections.emptyList();

                    taskRepository.updateTaskStatusSync(taskId, userId, TaskStatus.DONE, completionTimeUtc);
                    logger.debug(TAG, "Task " + taskId + " status updated to DONE.");

                    taskStatisticsRepository.updateCompletionTimeSync(taskId, completionTimeUtc);
                    logger.debug(TAG, "TaskStatistics for " + taskId + " (completionTime) updated.");

                    // Вызываем execute и дожидаемся
                    processTaskCompletionUseCase.execute(taskId, tags).get();
                    logger.info(TAG, "Main gamification for task " + taskId + " completion processed.");

                    return null;
                });
                return null;
            } catch (Exception e) {
                logger.error(TAG, "Failed to force complete task " + taskId, e);
                throw e;
            }
        }, ioExecutor);
    }
}