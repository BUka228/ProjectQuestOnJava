package com.example.projectquestonjava.feature.gamification.data.repository;

import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.RewardDao;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.repository.DailyRewardRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DailyRewardRepositoryImpl implements DailyRewardRepository {

    private static final String TAG = "DailyRewardRepository";
    private final RewardDao rewardDao;
    private final Logger logger;
    private final Executor ioExecutor;

    @Inject
    public DailyRewardRepositoryImpl(
            RewardDao rewardDao,
            Logger logger,
            @com.example.projectquestonjava.core.di.IODispatcher Executor ioExecutor) {
        this.rewardDao = rewardDao;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public ListenableFuture<Reward> getRewardById(long rewardId) {
        logger.debug(TAG, "Getting reward by id=" + rewardId);
        return Futures.catchingAsync(
                rewardDao.getById(rewardId), // DAO возвращает ListenableFuture
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting reward by id=" + rewardId, e);
                    return null; // или throw
                },
                ioExecutor // Указываем Executor для коллбэка catchingAsync
        );
    }
}