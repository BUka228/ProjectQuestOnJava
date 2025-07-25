package com.example.projectquestonjava.feature.statistics.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.statistics.data.dao.TaskStatisticsDao;
import com.example.projectquestonjava.feature.statistics.data.model.TaskStatistics;
import com.example.projectquestonjava.feature.statistics.domain.repository.TaskStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskStatisticsRepositoryImpl implements TaskStatisticsRepository {

    private static final String TAG = "TaskStatisticsRepo";
    private final TaskStatisticsDao taskStatisticsDao;
    private final UserSessionManager userSessionManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public TaskStatisticsRepositoryImpl(
            TaskStatisticsDao taskStatisticsDao,
            UserSessionManager userSessionManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskStatisticsDao = taskStatisticsDao;
        this.userSessionManager = userSessionManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    private <T> ListenableFuture<T> executeWithUserCheck(UserFunction<T> function) {
        int userId = userSessionManager.getUserIdSync();
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.warn(TAG, "User not logged in. Operation aborted.");
            return Futures.immediateFailedFuture(new IllegalStateException("User not logged in"));
        }
        return function.apply(userId);
    }

    private <T> LiveData<T> executeWithUserCheckLiveData(UserFunctionLiveData<T> function, T defaultValue) {
        // LiveData будет обновляться при изменении user ID
        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "User not logged in for LiveData. Returning default.");
                return new LiveData<T>(defaultValue) {};
            }
            return function.apply(userId);
        });
    }


    @Override
    public ListenableFuture<TaskStatistics> getStatisticsForTask(long taskId) {
        return executeWithUserCheck(userId -> {
            logger.debug(TAG, "Getting statistics suspend for taskId=" + taskId + ", userId=" + userId);
            return Futures.catchingAsync(
                    taskStatisticsDao.getStatisticsForTaskSuspend(taskId, userId),
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Error getting statistics for taskId=" + taskId, e);
                        return null; // или throw
                    },
                    ioExecutor
            );
        });
    }

    @Override
    public LiveData<TaskStatistics> getStatisticsForTaskFlow(long taskId) {
        logger.debug(TAG, "Getting statistics LiveData for taskId=" + taskId);
        return executeWithUserCheckLiveData(userId -> taskStatisticsDao.getTaskStatistics(taskId, userId), null);
    }

    @Override
    public LiveData<List<TaskStatistics>> getStatisticsForTasksFlow(List<Long> taskIds) {
        logger.debug(TAG, "Getting statistics LiveData for taskIds=" + taskIds);
        if (taskIds == null || taskIds.isEmpty()) {
            return new LiveData<List<TaskStatistics>>(Collections.emptyList()) {};
        }
        return executeWithUserCheckLiveData(userId -> taskStatisticsDao.getStatisticsForTasks(taskIds, userId), Collections.emptyList());
    }

    @Override
    public ListenableFuture<Long> insertOrUpdateStatistics(TaskStatistics statistics) {
        logger.debug(TAG, "Inserting/Updating statistics for taskId=" + statistics.getTaskId());
        return Futures.catchingAsync(
                taskStatisticsDao.insertOrUpdateTaskStatistics(statistics),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error inserting/updating statistics for taskId=" + statistics.getTaskId(), e);
                    throw new RuntimeException(e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> incrementCompletedPomodoroFocusSessions(long taskId) {
        logger.debug(TAG, "Incrementing completed pomodoro FOCUS SESSIONS for taskId=" + taskId);
        ListenableFuture<Integer> futureResult = taskStatisticsDao.incrementCompletedPomodoroFocusSessions(taskId);
        return Futures.transform(futureResult, result -> null, ioExecutor);
    }

    @Override
    public ListenableFuture<Void> addTotalPomodoroFocusTime(long taskId, int secondsToAdd) {
        if (secondsToAdd <= 0) return Futures.immediateFuture(null);
        logger.debug(TAG, "Adding " + secondsToAdd + " seconds to total pomodoro FOCUS TIME for task " + taskId);
        ListenableFuture<Integer> futureResult = taskStatisticsDao.addTotalPomodoroFocusTime(taskId, secondsToAdd);
        return Futures.transform(futureResult, result -> null, ioExecutor);
    }

    @Override
    public ListenableFuture<Void> incrementTotalPomodoroInterruptions(long taskId, int countToAdd) {
        if (countToAdd <= 0) return Futures.immediateFuture(null);
        logger.debug(TAG, "Incrementing total pomodoro INTERRUPTIONS by " + countToAdd + " for task " + taskId);
        ListenableFuture<Integer> futureResult = taskStatisticsDao.incrementTotalPomodoroInterruptions(taskId, countToAdd);
        return Futures.transform(futureResult, result -> null, ioExecutor);
    }

    @Override
    public ListenableFuture<Void> markTaskAsCompletedOnce(long taskId) {
        logger.debug(TAG, "Marking task " + taskId + " as completed once.");
        ListenableFuture<Integer> futureResult = taskStatisticsDao.markTaskAsCompletedOnce(taskId);
        return Futures.transform(futureResult, result -> null, ioExecutor);
    }

    @Override
    public ListenableFuture<TaskStatistics> ensureAndGetStatistics(long taskId, TaskStatistics defaultStats) {
        return executeWithUserCheck(userId -> {
            ListenableFuture<TaskStatistics> existingStatsFuture = taskStatisticsDao.getStatisticsForTaskSuspend(taskId, userId);
            return Futures.transformAsync(existingStatsFuture, existingStats -> {
                if (existingStats != null) {
                    logger.debug(TAG, "Statistics for task " + taskId + " (user " + userId + ") already exist. Returning existing.");
                    return Futures.immediateFuture(existingStats);
                } else {
                    logger.debug(TAG, "Statistics for task " + taskId + " (user " + userId + ") do not exist. Creating with defaults.");
                    TaskStatistics statsToInsert = new TaskStatistics(taskId, defaultStats.getCompletionTime(), defaultStats.getTimeSpentSeconds(), defaultStats.getTotalPomodoroFocusSeconds(), defaultStats.getCompletedPomodoroFocusSessions(), defaultStats.getTotalPomodoroInterruptions(), defaultStats.isWasCompletedOnce());
                    return Futures.transform(taskStatisticsDao.insertOrUpdateTaskStatistics(statsToInsert), id -> statsToInsert, ioExecutor);
                }
            }, ioExecutor);
        });
    }

    @Override
    public ListenableFuture<Void> deleteStatisticsForTask(long taskId) {
        logger.debug(TAG, "Deleting statistics for taskId=" + taskId);
        ListenableFuture<Integer> futureResult = taskStatisticsDao.deleteStatisticsForTask(taskId);
        return Futures.transform(futureResult, result -> null, ioExecutor);
    }

    @Override
    public ListenableFuture<Void> addTimeToSpent(long taskId, int secondsToAdd) {
        if (secondsToAdd <= 0) return Futures.immediateFuture(null);
        logger.debug(TAG, "Adding " + secondsToAdd + " seconds to general time spent for task " + taskId);
        ListenableFuture<Integer> futureResult = taskStatisticsDao.addTimeToSpent(taskId, secondsToAdd);
        return Futures.transform(futureResult, result -> null, ioExecutor);
    }

    @Override
    public ListenableFuture<Void> updateCompletionTime(long taskId, LocalDateTime completionTime) {
        logger.debug(TAG, "Updating completion time for task " + taskId + " to " + completionTime);
        ListenableFuture<Integer> futureResult = taskStatisticsDao.updateCompletionTime(taskId, completionTime);
        return Futures.transform(futureResult, result -> null, ioExecutor);
    }

    @Override
    public LiveData<List<TaskStatistics>> getAllTaskStatisticsFlow() {
        logger.debug(TAG, "Getting all task statistics LiveData for current user");
        return executeWithUserCheckLiveData(taskStatisticsDao::getAllTaskStatistics, Collections.emptyList());
    }

    @Override
    public ListenableFuture<List<TaskStatistics>> getTaskStatsInPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        return executeWithUserCheck(userId -> {
            logger.debug(TAG, "Getting task stats for period: " + startTime + " - " + endTime + " (userId=" + userId + ")");
            return taskStatisticsDao.getTaskStatsInPeriod(userId, startTime, endTime);
        });
    }

    @Override
    public ListenableFuture<List<TaskStatistics>> getAllTaskStatisticsSuspend() {
        return executeWithUserCheck(userId -> {
            logger.debug(TAG, "Getting all task statistics suspend for userId=" + userId);
            return taskStatisticsDao.getAllTaskStatisticsSuspend(userId);
        });
    }


    // --- РЕАЛИЗАЦИИ SYNC МЕТОДОВ ---
    @Override
    public void addTimeToSpentSync(long taskId, int secondsToAdd) {
        if (secondsToAdd <= 0) return;
        logger.debug(TAG, "SYNC Adding " + secondsToAdd + "s to time spent for task " + taskId);
        taskStatisticsDao.addTimeToSpentSync(taskId, secondsToAdd);
    }

    @Override
    public void addTotalPomodoroFocusTimeSync(long taskId, int secondsToAdd) {
        if (secondsToAdd <= 0) return;
        logger.debug(TAG, "SYNC Adding " + secondsToAdd + "s to Pomodoro focus time for task " + taskId);
        taskStatisticsDao.addTotalPomodoroFocusTimeSync(taskId, secondsToAdd);
    }

    @Override
    public void incrementCompletedPomodoroFocusSessionsSync(long taskId) {
        logger.debug(TAG, "SYNC Incrementing completed Pomodoro focus sessions for task " + taskId);
        taskStatisticsDao.incrementCompletedPomodoroFocusSessionsSync(taskId);
    }

    @Override
    public void incrementTotalPomodoroInterruptionsSync(long taskId, int countToAdd) {
        if (countToAdd <= 0) return;
        logger.debug(TAG, "SYNC Incrementing Pomodoro interruptions by " + countToAdd + " for task " + taskId);
        taskStatisticsDao.incrementTotalPomodoroInterruptionsSync(taskId, countToAdd);
    }

    @Override
    public void markTaskAsCompletedOnceSync(long taskId) {
        logger.debug(TAG, "SYNC Marking task " + taskId + " as completed once.");
        taskStatisticsDao.markTaskAsCompletedOnceSync(taskId);
    }

    @Override
    public void updateCompletionTimeSync(long taskId, LocalDateTime completionTime) {
        logger.debug(TAG, "SYNC Updating completion time for task " + taskId + " to " + completionTime);
        taskStatisticsDao.updateCompletionTimeSync(taskId, completionTime);
    }

    @Override
    public TaskStatistics ensureAndGetStatisticsSync(long taskId, TaskStatistics defaultStats) {
        // Этот метод требует проверки userId, которая должна быть в DAO или здесь, если userId не часть defaultStats
        // Для простоты, предполагаем, что TaskStatisticsDao.getStatisticsForTaskSyncDirect не требует userId.
        logger.debug(TAG, "SYNC Ensuring statistics for taskId=" + taskId);
        TaskStatistics existing = taskStatisticsDao.getStatisticsForTaskSyncDirect(taskId);
        if (existing != null) {
            return existing;
        } else {
            TaskStatistics statsToInsert = new TaskStatistics(taskId, defaultStats.getCompletionTime(), defaultStats.getTimeSpentSeconds(), defaultStats.getTotalPomodoroFocusSeconds(), defaultStats.getCompletedPomodoroFocusSessions(), defaultStats.getTotalPomodoroInterruptions(), defaultStats.isWasCompletedOnce());
            taskStatisticsDao.insertOrUpdateTaskStatisticsSync(statsToInsert); // DAO возвращает long, но нам нужен объект
            return statsToInsert; // Возвращаем созданный объект
        }
    }

    @Override
    public TaskStatistics getStatisticsForTaskSync(long taskId) {
        int userId = userSessionManager.getUserIdSync();
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.warn(TAG, "SYNC Cannot get statistics for task " + taskId + ": User not logged in.");
            return null; // или бросить исключение
        }
        logger.debug(TAG, "SYNC Getting statistics for taskId=" + taskId + ", userId=" + userId);
        // Используем getStatisticsForTaskSuspend из DAO, но вызываем его синхронно (т.к. мы на ioExecutor)
        // Если в DAO нет getStatisticsForTaskSync(taskId, userId), то нужно его добавить
        // или адаптировать существующий suspend метод DAO для синхронного вызова, если это возможно.
        // Предположим, что taskStatisticsDao.getStatisticsForTaskSuspend(taskId, userId) возвращает ListenableFuture
        // и мы хотим его выполнить синхронно.
        try {
            // Если getStatisticsForTaskSuspend из DAO возвращает ListenableFuture, то:
            return taskStatisticsDao.getStatisticsForTaskSuspend(taskId, userId).get();
            // Если в DAO есть прямой синхронный метод getStatisticsForTaskSync(taskId, userId), то:
            // return taskStatisticsDao.getStatisticsForTaskSync(taskId, userId);
        } catch (Exception e) {
            logger.error(TAG, "Error SYNC getting statistics for taskId=" + taskId, e);
            return null; // или throw
        }
    }

    // Вспомогательные интерфейсы для лямбд с userId
    @FunctionalInterface
    interface UserFunction<R> {
        ListenableFuture<R> apply(int userId);
    }

    @FunctionalInterface
    interface UserFunctionLiveData<R> {
        LiveData<R> apply(int userId);
    }
}