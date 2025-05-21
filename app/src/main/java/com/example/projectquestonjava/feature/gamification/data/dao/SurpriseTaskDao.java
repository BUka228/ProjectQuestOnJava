package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDate;
import java.util.List;

@Dao
public interface SurpriseTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(SurpriseTask surpriseTask);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<SurpriseTask> surpriseTasks);

    @Update
    ListenableFuture<Integer> update(SurpriseTask surpriseTask);

    @Delete
    ListenableFuture<Integer> delete(SurpriseTask surpriseTask);

    @Query("SELECT * FROM surprise_task WHERE gamification_id = :gamificationId AND shown_date = :date AND is_completed = 0 LIMIT 1")
    ListenableFuture<SurpriseTask> getActiveTaskForDate(long gamificationId, LocalDate date);

    @Query("SELECT * FROM surprise_task WHERE gamification_id = :gamificationId AND shown_date = :date AND is_completed = 0 LIMIT 1")
    LiveData<SurpriseTask> getActiveTaskForDateFlow(long gamificationId, LocalDate date);

    @Query("SELECT * FROM surprise_task WHERE gamification_id = :gamificationId AND is_completed = 0 AND shown_date IS NULL")
    ListenableFuture<List<SurpriseTask>> getAvailableUncompletedTasks(long gamificationId);

    @Query("SELECT * FROM surprise_task WHERE gamification_id = :gamificationId ORDER BY expiration_time DESC")
    LiveData<List<SurpriseTask>> getAllSurpriseTasksFlow(long gamificationId);

    @Query("DELETE FROM surprise_task WHERE gamification_id = :gamificationId")
    ListenableFuture<Integer> deleteSurpriseTasksForGamification(long gamificationId);
}