package com.example.projectquestonjava.feature.gamification.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDate;
import java.util.List;

public interface SurpriseTaskRepository {
    LiveData<SurpriseTask> getActiveTaskForDateFlow(LocalDate date);
    ListenableFuture<List<SurpriseTask>> getAvailableTasks(long gamificationId);
    ListenableFuture<Long> insertSurpriseTask(SurpriseTask task);
    ListenableFuture<Void> updateSurpriseTask(SurpriseTask task);
    ListenableFuture<Void> deleteSurpriseTasksForGamification(long gamificationId);
    ListenableFuture<SurpriseTask> getActiveTaskForDateFuture(long gamificationId, LocalDate date);
}