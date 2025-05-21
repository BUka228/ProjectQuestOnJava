package com.example.projectquestonjava.feature.pomodoro.domain.usecases;

import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.data.model.relations.TaskWithTags;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.domain.usecases.ProcessTaskCompletionUseCase;
import com.example.projectquestonjava.feature.pomodoro.domain.model.InterruptedPhaseInfo;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.statistics.domain.repository.TaskStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import androidx.annotation.Nullable; // Для InterruptedPhaseInfo



public class ForceCompleteTaskWithPomodoroUseCase {
    private static final String TAG = "ForceCompleteTaskWithPomodoroUC";

    private final TaskRepository taskRepository;
    private final TaskStatisticsRepository taskStatisticsRepository;
    private final CompletePomodoroSessionUseCase completePomodoroSessionUseCase;
    private final ProcessTaskCompletionUseCase processTaskCompletionUseCase;
    private final UnitOfWork unitOfWork;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public ForceCompleteTaskWithPomodoroUseCase(
            TaskRepository taskRepository,
            TaskStatisticsRepository taskStatisticsRepository,
            CompletePomodoroSessionUseCase completePomodoroSessionUseCase,
            ProcessTaskCompletionUseCase processTaskCompletionUseCase,
            UnitOfWork unitOfWork,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskRepository = taskRepository;
        this.taskStatisticsRepository = taskStatisticsRepository;
        this.completePomodoroSessionUseCase = completePomodoroSessionUseCase;
        this.processTaskCompletionUseCase = processTaskCompletionUseCase;
        this.unitOfWork = unitOfWork;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(long taskId, @Nullable InterruptedPhaseInfo interruptedPhaseInfo) {
        return Futures.submitAsync(() -> {
            logger.info(TAG, "Forcing completion for task " + taskId + ". Interrupted phase: " + (interruptedPhaseInfo != null));
            try {
                unitOfWork.withTransaction((Callable<Void>) () -> {
                    LocalDateTime completionTimeUtc = dateTimeUtils.currentUtcDateTime();

                    // 1. Если была активная Pomodoro-фаза, "завершаем" ее
                    if (interruptedPhaseInfo != null) {
                        long actualDurationSeconds = Duration.between(interruptedPhaseInfo.getStartTime(), completionTimeUtc)
                                .getSeconds(); // getSeconds возвращает long
                        actualDurationSeconds = Math.max(0, actualDurationSeconds); // Убедимся, что не отрицательное

                        logger.debug(TAG, "Processing interrupted Pomodoro phase " + interruptedPhaseInfo.getDbSessionId() +
                                " for task " + taskId + ". Type: " + interruptedPhaseInfo.getType() +
                                ", ActualDuration: " + actualDurationSeconds + "s, Interruptions: " + interruptedPhaseInfo.getInterruptions());

                        Futures.getDone(completePomodoroSessionUseCase.execute(
                                interruptedPhaseInfo.getDbSessionId(),
                                taskId,
                                interruptedPhaseInfo.getType(),
                                (int) actualDurationSeconds, // Приведение к int
                                interruptedPhaseInfo.getInterruptions()
                        ));
                        logger.debug(TAG, "Interrupted Pomodoro phase " + interruptedPhaseInfo.getDbSessionId()+ " processed.");
                    }

                    // 2. Получаем задачу с тегами (нужно для processTaskCompletionUseCase)
                    TaskWithTags taskWithTags = Futures.getDone(taskRepository.getTaskWithTagsById(taskId));
                    if (taskWithTags == null) {
                        throw new NoSuchElementException("Task " + taskId + " not found for completion.");
                    }
                    List<Tag> tags = taskWithTags.getTags() != null ? taskWithTags.getTags() : Collections.emptyList();


                    // 3. Обновляем статус основной задачи на DONE
                    Futures.getDone(taskRepository.updateTaskStatus(taskId, TaskStatus.DONE));
                    logger.debug(TAG, "Task " + taskId + " status updated to DONE.");

                    // 4. Обновляем основную статистику задачи
                    Futures.getDone(taskStatisticsRepository.updateCompletionTime(taskId, completionTimeUtc));
                    logger.debug(TAG, "TaskStatistics for " + taskId + " (completionTime) updated.");

                    // 5. Запускаем основную геймификацию за ЗАВЕРШЕНИЕ ЗАДАЧИ
                    Futures.getDone(processTaskCompletionUseCase.execute(taskId, tags));
                    logger.info(TAG, "Main gamification for task " + taskId + " completion processed.");

                    return null; // Для Callable<Void>
                });
                return Futures.immediateFuture(null); // Успех
            } catch (Exception e) {
                logger.error(TAG, "Failed to force complete task " + taskId, e);
                return Futures.immediateFailedFuture(e); // Провал
            }
        }, ioExecutor);
    }
}