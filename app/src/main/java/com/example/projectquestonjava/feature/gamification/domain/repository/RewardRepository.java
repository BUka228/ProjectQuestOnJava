package com.example.projectquestonjava.feature.gamification.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

public interface RewardRepository {
    ListenableFuture<Reward> getRewardById(long rewardId);
    ListenableFuture<List<Reward>> getAllRewards();
    LiveData<List<Reward>> getAllRewardsFlow();
    ListenableFuture<Long> insertReward(Reward reward);
    ListenableFuture<Void> insertAllRewards(List<Reward> rewards);
    ListenableFuture<Void> updateReward(Reward reward);
    ListenableFuture<Void> deleteReward(Reward reward);
}