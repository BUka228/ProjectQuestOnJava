package com.example.projectquestonjava.feature.statistics.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.projectquestonjava.feature.statistics.data.model.TaskStatistics;
import com.google.common.util.concurrent.ListenableFuture; // Для асинхронных операций
import java.time.LocalDateTime;
import java.util.List;

public interface TaskStatisticsRepository {

    ListenableFuture<TaskStatistics> getStatisticsForTask(long taskId);

    LiveData<TaskStatistics> getStatisticsForTaskFlow(long taskId); // LiveData вместо Flow

    LiveData<List<TaskStatistics>> getStatisticsForTasksFlow(List<Long> taskIds); // LiveData вместо Flow

    ListenableFuture<Long> insertOrUpdateStatistics(TaskStatistics statistics);

    ListenableFuture<Void> incrementCompletedPomodoroFocusSessions(long taskId);

    ListenableFuture<Void> addTotalPomodoroFocusTime(long taskId, int secondsToAdd);

    ListenableFuture<Void> incrementTotalPomodoroInterruptions(long taskId, int countToAdd);

    ListenableFuture<Void> markTaskAsCompletedOnce(long taskId);

    ListenableFuture<TaskStatistics> ensureAndGetStatistics(long taskId, TaskStatistics defaultStats);

    ListenableFuture<Void> deleteStatisticsForTask(long taskId);

    ListenableFuture<Void> addTimeToSpent(long taskId, int secondsToAdd);

    ListenableFuture<Void> updateCompletionTime(long taskId, LocalDateTime completionTime);

    LiveData<List<TaskStatistics>> getAllTaskStatisticsFlow(); // LiveData вместо Flow

    ListenableFuture<List<TaskStatistics>> getTaskStatsInPeriod(LocalDateTime startTime, LocalDateTime endTime);

    ListenableFuture<List<TaskStatistics>> getAllTaskStatisticsSuspend();


    void addTimeToSpentSync(long taskId, int secondsToAdd); // Уже был
    void addTotalPomodoroFocusTimeSync(long taskId, int secondsToAdd); // Уже был
    void incrementCompletedPomodoroFocusSessionsSync(long taskId); // Уже был
    void incrementTotalPomodoroInterruptionsSync(long taskId, int countToAdd); // Уже был
    void markTaskAsCompletedOnceSync(long taskId); // Уже был
    void updateCompletionTimeSync(long taskId, LocalDateTime completionTime); // Уже был
    TaskStatistics ensureAndGetStatisticsSync(long taskId, TaskStatistics defaultStats); // Уже был
    TaskStatistics getStatisticsForTaskSync(long taskId);


}