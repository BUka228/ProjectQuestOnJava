package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.projectquestonjava.feature.gamification.data.model.StreakRewardDefinition;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface StreakRewardDefinitionDao {
    @Query("SELECT * FROM streak_reward_definition WHERE streak_day = :streakDay LIMIT 1")
    ListenableFuture<StreakRewardDefinition> getRewardDefinitionForStreak(int streakDay);

    @Query("SELECT * FROM streak_reward_definition WHERE streak_day BETWEEN :startStreak AND :endStreak ORDER BY streak_day ASC")
    ListenableFuture<List<StreakRewardDefinition>> getRewardDefinitionsForStreakRange(int startStreak, int endStreak);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insert(StreakRewardDefinition definition);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<StreakRewardDefinition> definitions);
}