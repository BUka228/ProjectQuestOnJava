package com.example.projectquestonjava.feature.statistics.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.statistics.data.dao.GamificationHistoryDao;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
import com.example.projectquestonjava.feature.statistics.domain.repository.GamificationHistoryRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GamificationHistoryRepositoryImpl implements GamificationHistoryRepository {

    private static final String TAG = "GamificationHistoryRepo";
    private final GamificationHistoryDao gamificationHistoryDao;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public GamificationHistoryRepositoryImpl(
            GamificationHistoryDao gamificationHistoryDao,
            GamificationDataStoreManager gamificationDataStoreManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.gamificationHistoryDao = gamificationHistoryDao;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    private <T> ListenableFuture<T> executeWithGamificationId(GamificationIdFunction<T> function) {
        return Futures.transformAsync(
                gamificationDataStoreManager.getGamificationIdFuture(),
                gamificationId -> {
                    if (gamificationId == null || gamificationId == -1L) {
                        return Futures.immediateFailedFuture(new IllegalStateException("Gamification ID not found"));
                    }
                    return function.apply(gamificationId);
                },
                ioExecutor
        );
    }

    @Override
    public LiveData<List<GamificationHistory>> getHistoryFlow() {
        logger.debug(TAG, "Getting history LiveData for current user");
        return Transformations.switchMap(gamificationDataStoreManager.getGamificationIdFlow(), gamificationId -> {
            if (gamificationId == null || gamificationId == -1L) {
                logger.warn(TAG, "No gamification ID found for history LiveData.");
                return new LiveData<List<GamificationHistory>>(Collections.emptyList()) {};
            }
            return gamificationHistoryDao.getHistoryForGamificationFlow(gamificationId);
        });
    }

    @Override
    public ListenableFuture<List<GamificationHistory>> getHistoryForPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        return executeWithGamificationId(gamiId -> {
            logger.debug(TAG, "Getting history for period: " + startTime + " - " + endTime + " (gamiId=" + gamiId + ")");
            return Futures.catchingAsync(
                    gamificationHistoryDao.getHistoryForGamificationInPeriod(gamiId, startTime, endTime),
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Error getting history for period", e);
                        return null;
                    },
                    ioExecutor
            );
        });
    }

    @Override
    public ListenableFuture<Long> insertHistoryEntry(GamificationHistory entry) {
        return executeWithGamificationId(currentGamificationId -> {
            if (entry.getGamificationId() != currentGamificationId) {
                logger.error(TAG, "Cannot insert history entry: Gamification ID mismatch. EntryGamiID=" + entry.getGamificationId() + ", CurrentGamiID=" + currentGamificationId);
                return Futures.immediateFailedFuture(new IllegalStateException("Gamification ID mismatch"));
            }
            logger.debug(TAG, "Inserting history entry: reason=" + entry.getReason());
            return Futures.catchingAsync(
                    gamificationHistoryDao.insert(entry),
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Error inserting history entry", e);
                        throw new RuntimeException("Insert history entry failed", e);
                    },
                    ioExecutor
            );
        });
    }

    @Override
    public ListenableFuture<Void> deleteHistoryForGamification() {
        return executeWithGamificationId(gamiId -> {
            logger.debug(TAG, "Deleting ALL history for gamificationId=" + gamiId);
            ListenableFuture<Integer> deleteFuture = gamificationHistoryDao.deleteHistoryForGamification(gamiId);
            return Futures.transform(deleteFuture, count -> null, MoreExecutors.directExecutor());
        });
    }

    // Вспомогательный функциональный интерфейс
    @FunctionalInterface
    private interface GamificationIdFunction<R> {
        ListenableFuture<R> apply(long gamificationId);
    }
}