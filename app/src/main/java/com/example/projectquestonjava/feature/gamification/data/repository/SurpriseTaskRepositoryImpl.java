package com.example.projectquestonjava.feature.gamification.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.SurpriseTaskDao;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.example.projectquestonjava.feature.gamification.domain.repository.SurpriseTaskRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SurpriseTaskRepositoryImpl implements SurpriseTaskRepository {

    private static final String TAG = "SurpriseTaskRepository";
    private final SurpriseTaskDao surpriseTaskDao;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public SurpriseTaskRepositoryImpl(
            SurpriseTaskDao surpriseTaskDao,
            GamificationDataStoreManager gamificationDataStoreManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.surpriseTaskDao = surpriseTaskDao;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    @Override
    public LiveData<SurpriseTask> getActiveTaskForDateFlow(LocalDate date) {
        logger.debug(TAG, "Getting active surprise task LiveData for date=" + date);
        return Transformations.switchMap(gamificationDataStoreManager.getGamificationIdFlow(), gamificationId -> {
            if (gamificationId == null || gamificationId == -1L) {
                logger.warn(TAG, "Cannot get surprise task, no gamification ID.");
                return new LiveData<SurpriseTask>(null) {};
            }
            return surpriseTaskDao.getActiveTaskForDateFlow(gamificationId, date);
        });
    }

    @Override
    public ListenableFuture<List<SurpriseTask>> getAvailableTasks(long gamificationId) {
        logger.debug(TAG, "Getting available surprise tasks for gamiId=" + gamificationId);
        return Futures.transform(
                surpriseTaskDao.getAvailableUncompletedTasks(gamificationId),
                tasks -> {
                    if (tasks == null) return Collections.emptyList();
                    LocalDateTime now = LocalDateTime.now();
                    List<SurpriseTask> filtered = tasks.stream()
                            .filter(task -> task.getExpirationTime().isAfter(now))
                            .collect(Collectors.toList());
                    logger.debug(TAG, "Found " + filtered.size() + " available (not expired) surprise tasks for gamiId=" + gamificationId);
                    return filtered;
                },
                ioExecutor // Выполняем фильтрацию на ioExecutor
        );
    }

    @Override
    public ListenableFuture<Long> insertSurpriseTask(SurpriseTask task) {
        logger.debug(TAG, "Inserting surprise task: gamiId=" + task.getGamificationId() + ", desc=" + task.getDescription());
        return surpriseTaskDao.insert(task);
    }

    @Override
    public ListenableFuture<Void> updateSurpriseTask(SurpriseTask task) {
        logger.debug(TAG, "Updating surprise task: id=" + task.getId() + ", completed=" + task.isCompleted() + ", shown=" + task.getShownDate());
        return Futures.transform(surpriseTaskDao.update(task), count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Void> deleteSurpriseTasksForGamification(long gamificationId) {
        logger.debug(TAG, "Deleting surprise tasks for gamificationId=" + gamificationId);
        return Futures.transform(surpriseTaskDao.deleteSurpriseTasksForGamification(gamificationId), count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<SurpriseTask> getActiveTaskForDateFuture(long gamificationId, LocalDate date) {
        logger.debug(TAG, "Getting active surprise task future for gamiId=" + gamificationId + ", date=" + date);
        // Проверка gamificationId не нужна, т.к. передается явно
        return Futures.catchingAsync(
                surpriseTaskDao.getActiveTaskForDate(gamificationId, date), // Вызов нового DAO метода
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting active task future for gamiId=" + gamificationId + ", date=" + date, e);
                    return null; // Возвращаем null в случае ошибки
                },
                ioExecutor
        );
    }
}