package com.example.projectquestonjava.feature.statistics.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.projectquestonjava.feature.statistics.data.model.GlobalStatistics;
import com.google.common.util.concurrent.ListenableFuture;

public interface GlobalStatisticsRepository {
    LiveData<GlobalStatistics> getGlobalStatisticsFlow();
    ListenableFuture<GlobalStatistics> getGlobalStatisticsSuspend();
    ListenableFuture<Void> incrementTotalTasks();
    ListenableFuture<Void> incrementCompletedTasks();
    ListenableFuture<Void> incrementTotalWorkspaces();
    ListenableFuture<Void> addTotalTimeSpent(int timeToAdd);
    ListenableFuture<Void> updateLastActive();

    void incrementTotalTasksSync(); // Для CreateCalendarTaskUseCase
    // При необходимости можно добавить и другие Sync методы
    void insertOrUpdateGlobalStatisticsSync(GlobalStatistics globalStatistics); // Для TestDataInitializer
    void incrementCompletedTasksSync();
    void addTotalTimeSpentSync(int timeToAdd);
    void updateLastActiveSync();
}