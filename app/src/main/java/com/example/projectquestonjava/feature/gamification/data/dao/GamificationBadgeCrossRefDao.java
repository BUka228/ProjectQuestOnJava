package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface GamificationBadgeCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insert(GamificationBadgeCrossRef crossRef);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<GamificationBadgeCrossRef> crossRefs);

    @Update
    ListenableFuture<Integer> update(GamificationBadgeCrossRef crossRef);

    @Delete
    ListenableFuture<Integer> delete(GamificationBadgeCrossRef crossRef);

    @Query("SELECT * FROM gamification_badge_cross_ref WHERE gamification_id = :gamificationId")
    LiveData<List<GamificationBadgeCrossRef>> getEarnedBadgesFlow(long gamificationId);

    @Query("SELECT * FROM gamification_badge_cross_ref WHERE gamification_id = :gamificationId")
    ListenableFuture<List<GamificationBadgeCrossRef>> getEarnedBadges(long gamificationId);

    @Query("DELETE FROM gamification_badge_cross_ref WHERE gamification_id = :gamificationId")
    ListenableFuture<Integer> deleteEarnedBadgesForGamification(long gamificationId);

    @Query("DELETE FROM gamification_badge_cross_ref WHERE badge_id = :badgeId")
    ListenableFuture<Integer> deleteEarnedBadgesForBadge(long badgeId);
}