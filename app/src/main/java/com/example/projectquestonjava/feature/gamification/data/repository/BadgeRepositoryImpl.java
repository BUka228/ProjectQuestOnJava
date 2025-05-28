package com.example.projectquestonjava.feature.gamification.data.repository;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.BadgeDao;
import com.example.projectquestonjava.feature.gamification.data.dao.GamificationBadgeCrossRefDao;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.example.projectquestonjava.feature.gamification.domain.repository.BadgeRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor; // Для асинхронных операций
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BadgeRepositoryImpl implements BadgeRepository {

    private static final String TAG = "BadgeRepositoryImpl";
    private final BadgeDao badgeDao;
    private final GamificationBadgeCrossRefDao gamificationBadgeCrossRefDao;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final Logger logger;
    private final Executor ioExecutor;

    @Inject
    public BadgeRepositoryImpl(
            BadgeDao badgeDao,
            GamificationBadgeCrossRefDao gamificationBadgeCrossRefDao,
            GamificationDataStoreManager gamificationDataStoreManager,
            Logger logger,
            @com.example.projectquestonjava.core.di.IODispatcher Executor ioExecutor) {
        this.badgeDao = badgeDao;
        this.gamificationBadgeCrossRefDao = gamificationBadgeCrossRefDao;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public LiveData<List<Badge>> getAllBadgesFlow() {
        logger.debug(TAG, "Getting all badges LiveData");
        return badgeDao.getAllBadgesFlow(); // DAO уже возвращает LiveData
    }

    @Override
    public ListenableFuture<List<Badge>> getAllBadges() {
        logger.debug(TAG, "Getting all badges future");
        return Futures.catchingAsync(
                badgeDao.getAllBadges(),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting all badges future", e);
                    throw new RuntimeException("Failed to get all badges", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Badge> getBadgeById(long badgeId) {
        logger.debug(TAG, "Getting badge by id=" + badgeId);
        return Futures.catchingAsync(
                badgeDao.getBadgeById(badgeId),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting badge by id=" + badgeId, e);
                    return null; // Или throw, если null не ожидается
                },
                ioExecutor
        );
    }

    @Override
    public LiveData<List<GamificationBadgeCrossRef>> getEarnedBadgesFlow() {
        logger.debug(TAG, "Getting earned badges LiveData for current user");
        // Используем Transformations.switchMap для реакции на изменение gamificationId
        return Transformations.switchMap(gamificationDataStoreManager.getGamificationIdFlow(), gamificationId -> {
            if (gamificationId == null || gamificationId == -1L) {
                logger.warn(TAG, "No gamification ID found for earned badges LiveData.");
                return new LiveData<List<GamificationBadgeCrossRef>>(Collections.emptyList()) {};
            }
            return gamificationBadgeCrossRefDao.getEarnedBadgesFlow(gamificationId);
        });
    }

    @Override
    public ListenableFuture<List<GamificationBadgeCrossRef>> getEarnedBadges() {
        logger.debug(TAG, "Getting earned badges future for current user");
        return Futures.transformAsync(
                gamificationDataStoreManager.getGamificationIdFuture(), // Получаем Future<Long>
                gamificationId -> {
                    if (gamificationId == null || gamificationId == -1L) {
                        logger.warn(TAG, "No gamification ID found for getting earned badges.");
                        return Futures.immediateFuture(Collections.emptyList());
                    }
                    return Futures.catchingAsync(
                            gamificationBadgeCrossRefDao.getEarnedBadges(gamificationId),
                            Exception.class,
                            e -> {
                                logger.error(TAG, "Error getting earned badges for gamiId=" + gamificationId, e);
                                throw new RuntimeException("Failed to get earned badges", e);
                            },
                            ioExecutor
                    );
                },
                ioExecutor // Executor для transformAsync
        );
    }

    @Override
    public ListenableFuture<Void> insertEarnedBadge(GamificationBadgeCrossRef crossRef) {
        logger.debug(TAG, "Inserting earned badge: gamiId=" + crossRef.getGamificationId() + ", badgeId=" + crossRef.getBadgeId());
        // DAO возвращает ListenableFuture<Void>, просто передаем его
        return Futures.catchingAsync(
                gamificationBadgeCrossRefDao.insert(crossRef),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error inserting earned badge", e);
                    throw new RuntimeException("Failed to insert earned badge", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> deleteEarnedBadgesForGamification(long gamificationId) {
        logger.debug(TAG, "Deleting earned badges for gamificationId=" + gamificationId);
        ListenableFuture<Integer> deleteFuture = gamificationBadgeCrossRefDao.deleteEarnedBadgesForGamification(gamificationId);
        return Futures.transform(deleteFuture, count -> null, MoreExecutors.directExecutor());
    }

    // --- SYNC ---
    @Override
    public void insertEarnedBadgeSync(GamificationBadgeCrossRef crossRef) {
        logger.debug(TAG, "SYNC Inserting earned badge: gamiId=" + crossRef.getGamificationId() + ", badgeId=" + crossRef.getBadgeId());
        try {
            gamificationBadgeCrossRefDao.insertSync(crossRef);
        } catch (Exception e) {
            logger.error(TAG, "Error SYNC inserting earned badge", e);
            throw new RuntimeException("Failed to SYNC insert earned badge", e);
        }
    }

    @Override
    public Badge getBadgeByIdSync(long badgeId) {
        logger.debug(TAG, "SYNC Getting badge by id=" + badgeId);
        try {
            return badgeDao.getBadgeByIdSync(badgeId);
        } catch (Exception e) {
            logger.error(TAG, "Error SYNC getting badge by id=" + badgeId, e);
            return null; // или throw
        }
    }


    @Override
    public List<GamificationBadgeCrossRef> getEarnedBadgesSync(long gamificationId) {
        logger.debug(TAG, "SYNC Getting earned badges for gamificationId=" + gamificationId);
        // DAO должен иметь getEarnedBadgesSync(gamificationId)
        try {
            return gamificationBadgeCrossRefDao.getEarnedBadgesSync(gamificationId);
        } catch (Exception e) {
            logger.error(TAG, "Error SYNC getting earned badges for gamiId=" + gamificationId, e);
            return Collections.emptyList();
        }
    }
}