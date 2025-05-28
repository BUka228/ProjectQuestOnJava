package com.example.projectquestonjava.feature.gamification.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.GamificationDao;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GamificationRepositoryImpl implements GamificationRepository {

    private static final String TAG = "GamificationRepository";
    private static final int BASE_EXPERIENCE = 100; // Базовый опыт для расчета следующего уровня

    private final GamificationDao gamificationDao;
    private final UserSessionManager userSessionManager;
    private final Logger logger;
    private final Executor ioExecutor;

    @Inject
    public GamificationRepositoryImpl(
            GamificationDao gamificationDao,
            UserSessionManager userSessionManager,
            Logger logger,
            @com.example.projectquestonjava.core.di.IODispatcher Executor ioExecutor) {
        this.gamificationDao = gamificationDao;
        this.userSessionManager = userSessionManager;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public LiveData<Gamification> getCurrentUserGamificationFlow() {
        logger.debug(TAG, "Getting current user gamification LiveData");
        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "No user logged in, returning empty gamification LiveData.");
                return new LiveData<Gamification>(null) {}; // Пустой LiveData
            }
            return gamificationDao.getByUserIdFlow(userId);
        });
    }

    @Override
    public ListenableFuture<Gamification> getGamificationByUserId(int userId) {
        logger.debug(TAG, "Getting gamification by userId=" + userId);
        return Futures.catchingAsync(
                gamificationDao.getByUserId(userId),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting gamification by userId=" + userId, e);
                    return null;
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Gamification> getGamificationById(long id) {
        logger.debug(TAG, "Getting gamification by gamificationId=" + id);
        return Futures.catchingAsync(
                gamificationDao.getById(id),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting gamification by gamificationId=" + id, e);
                    return null;
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Long> insertGamification(Gamification gamification) {
        logger.debug(TAG, "Inserting gamification for userId=" + gamification.getUserId());
        return Futures.catchingAsync(
                gamificationDao.insert(gamification),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error inserting gamification for userId=" + gamification.getUserId(), e);
                    throw new RuntimeException("Failed to insert gamification", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> updateGamification(Gamification gamification) {
        logger.debug(TAG, "Updating gamification for userId=" + gamification.getUserId() + ", level=" + gamification.getLevel() + ", xp=" + gamification.getExperience());
        Gamification updatedGamification = recalculateGamificationIfNeeded(gamification);
        ListenableFuture<Integer> updateFuture = gamificationDao.update(updatedGamification);
        return Futures.transform(updateFuture, count -> {
            logger.debug(TAG, "Gamification updated for userId=" + gamification.getUserId() + ". New state: level=" + updatedGamification.getLevel() + ", xp=" + updatedGamification.getExperience());
            return null;
        }, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Gamification> getCurrentUserGamificationFuture() {
        logger.debug(TAG, "Getting current user gamification ListenableFuture");
        // UserSessionManager должен иметь метод, возвращающий ListenableFuture<Integer>
        // Предположим, такой метод есть, или мы адаптируем userSessionManager.getUserIdSync()
        // для вызова внутри submitAsync.
        // Пока используем getUserIdSync() внутри submitAsync для простоты,
        // но в идеале UserSessionManager.getUserIdFuture()
        return Futures.submitAsync(() -> {
            int userId = userSessionManager.getUserIdSync();
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "No user logged in for getCurrentUserGamificationFuture.");
                return Futures.immediateFuture(null);
            }
            return gamificationDao.getByUserId(userId);
        }, ioExecutor);
    }


    private Gamification recalculateGamificationIfNeeded(Gamification gamification) {
        Gamification current = gamification; // Создаем копию для изменения
        while (current.getExperience() >= current.getMaxExperienceForLevel()) {
            int xpOver = current.getExperience() - current.getMaxExperienceForLevel();
            int nextLevel = current.getLevel() + 1;
            int nextMaxXp = calculateMaxExperienceForLevel(nextLevel);
            // Создаем новый объект с обновленными значениями
            current = new Gamification(
                    current.getId(), current.getUserId(), nextLevel, xpOver, current.getCoins(),
                    nextMaxXp, current.getLastActive(), current.getCurrentStreak(),
                    current.getLastClaimedDate(), current.getMaxStreak()
            );
            logger.info(TAG, "Level up for userId=" + current.getUserId() + "! New level: " + current.getLevel() + ", MaxXP: " + nextMaxXp);
        }
        return current;
    }

    private int calculateMaxExperienceForLevel(int level) {
        int safeLevel = Math.max(1, level);
        return BASE_EXPERIENCE * safeLevel * safeLevel;
    }

    @Override
    public ListenableFuture<Void> deleteGamificationForUser(int userId) {
        logger.debug(TAG, "Deleting gamification data for userId=" + userId);
        ListenableFuture<Integer> deleteFuture = gamificationDao.deleteGamificationForUser(userId);
        return Futures.transform(deleteFuture, count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public Gamification getGamificationByIdSync(long id) {
        logger.debug(TAG, "SYNC Getting gamification by gamificationId=" + id);
        // DAO уже должен иметь getByIdSync(id)
        return gamificationDao.getByIdSync(id);
    }

    @Override
    public void updateGamificationSync(Gamification gamification) {
        logger.debug(TAG, "SYNC Updating gamification for userId=" + gamification.getUserId() + ", level=" + gamification.getLevel() + ", xp=" + gamification.getExperience());
        Gamification updatedGamification = recalculateGamificationIfNeeded(gamification); // Эта логика остается
        int updatedRows = gamificationDao.updateSync(updatedGamification); // DAO должен иметь updateSync
        if (updatedRows == 0) {
            logger.warn(TAG, "SYNC Gamification update affected 0 rows for gamificationId=" + gamification.getId());
        }
        logger.debug(TAG, "SYNC Gamification updated for userId=" + gamification.getUserId() + ". New state: level=" + updatedGamification.getLevel() + ", xp=" + updatedGamification.getExperience());
    }
}