package com.example.projectquestonjava.feature.gamification.domain.repository;

import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.google.common.util.concurrent.ListenableFuture;

public interface DailyRewardRepository {
    ListenableFuture<Reward> getRewardById(long rewardId);
}