package com.example.projectquestonjava.feature.gamification.data.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.RewardDao;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.repository.RewardRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RewardRepositoryImpl implements RewardRepository {

    private static final String TAG = "RewardRepositoryImpl";
    private final RewardDao rewardDao;
    private final Logger logger;
    private final Executor ioExecutor;

    @Inject
    public RewardRepositoryImpl(
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
                rewardDao.getById(rewardId),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting reward by id=" + rewardId, e);
                    return null;
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<List<Reward>> getAllRewards() {
        logger.debug(TAG, "Getting all rewards future");
        return Futures.catchingAsync(
                rewardDao.getAll(),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting all rewards future", e);
                    throw new RuntimeException("Failed to get all rewards", e);
                },
                ioExecutor
        );
    }

    @Override
    public LiveData<List<Reward>> getAllRewardsFlow() {
        logger.debug(TAG, "Getting all rewards LiveData");
        return rewardDao.getAllFlow();
    }

    @Override
    public ListenableFuture<Long> insertReward(Reward reward) {
        logger.debug(TAG, "Inserting reward: name=" + reward.getName());
        return Futures.catchingAsync(
                rewardDao.insert(reward),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error inserting reward", e);
                    throw new RuntimeException("Failed to insert reward", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> insertAllRewards(List<Reward> rewards) {
        logger.debug(TAG, "Inserting " + rewards.size() + " rewards");
        return Futures.catchingAsync(
                rewardDao.insertAll(rewards),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error inserting all rewards", e);
                    throw new RuntimeException("Failed to insert all rewards", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> updateReward(Reward reward) {
        logger.debug(TAG, "Updating reward: id=" + reward.getId() + ", name=" + reward.getName());
        ListenableFuture<Integer> future = rewardDao.update(reward);
        return Futures.transform(future, count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Void> deleteReward(Reward reward) {
        logger.debug(TAG, "Deleting reward: id=" + reward.getId() + ", name=" + reward.getName());
        ListenableFuture<Integer> future = rewardDao.delete(reward);
        return Futures.transform(future, count -> null, MoreExecutors.directExecutor());
    }
}