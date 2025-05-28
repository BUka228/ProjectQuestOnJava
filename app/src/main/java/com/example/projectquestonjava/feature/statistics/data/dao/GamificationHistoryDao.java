package com.example.projectquestonjava.feature.statistics.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
// AggregatedHistoryPoint не используется в DAO напрямую, а в ViewModel/Repository
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface GamificationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(GamificationHistory historyEntry);

    @Query("SELECT * FROM gamification_history WHERE gamification_id = :gamificationId ORDER BY timestamp DESC")
    LiveData<List<GamificationHistory>> getHistoryForGamificationFlow(long gamificationId);

    @Query("SELECT * FROM gamification_history WHERE gamification_id = :gamificationId ORDER BY timestamp DESC")
    ListenableFuture<List<GamificationHistory>> getHistoryForGamification(long gamificationId);

    @Query("SELECT * FROM gamification_history WHERE gamification_id = :gamificationId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    ListenableFuture<List<GamificationHistory>> getHistoryForGamificationInPeriod(long gamificationId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("DELETE FROM gamification_history WHERE gamification_id = :gamificationId")
    ListenableFuture<Integer> deleteHistoryForGamification(long gamificationId);

    // --- SYNC ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSync(GamificationHistory historyEntry);
}