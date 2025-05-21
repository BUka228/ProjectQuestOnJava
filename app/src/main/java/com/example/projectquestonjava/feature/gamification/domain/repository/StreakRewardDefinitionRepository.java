package com.example.projectquestonjava.feature.gamification.domain.repository;

import com.example.projectquestonjava.feature.gamification.data.model.StreakRewardDefinition;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

public interface StreakRewardDefinitionRepository {
    ListenableFuture<StreakRewardDefinition> getRewardDefinitionForStreak(int streakDay);
    ListenableFuture<List<StreakRewardDefinition>> getRewardDefinitionsForStreakRange(int startStreak, int endStreak);
    ListenableFuture<Void> insertDefinition(StreakRewardDefinition definition);
    ListenableFuture<Void> insertAllDefinitions(List<StreakRewardDefinition> definitions);
}