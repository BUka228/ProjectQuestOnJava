package com.example.projectquestonjava.feature.statistics.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.projectquestonjava.feature.statistics.data.model.GlobalStatistics;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;

@Dao
public interface GlobalStatisticsDao {
    @Query("SELECT * FROM global_statistics WHERE user_id = :userId LIMIT 1")
    LiveData<GlobalStatistics> getGlobalStatistics(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insertOrUpdateGlobalStatistics(GlobalStatistics globalStatistics);

    @Query("UPDATE global_statistics SET total_tasks = total_tasks + 1 WHERE user_id = :userId")
    ListenableFuture<Integer> incrementTotalTasks(int userId);

    @Query("UPDATE global_statistics SET completed_tasks = completed_tasks + 1 WHERE user_id = :userId")
    ListenableFuture<Integer> incrementCompletedTasks(int userId);

    @Query("UPDATE global_statistics SET total_workspaces = total_workspaces + 1 WHERE user_id = :userId")
    ListenableFuture<Integer> incrementTotalWorkspaces(int userId);

    @Query("UPDATE global_statistics SET total_time_spent = total_time_spent + :timeToAdd WHERE user_id = :userId")
    ListenableFuture<Integer> addTotalTimeSpent(int userId, int timeToAdd);

    @Query("UPDATE global_statistics SET last_active = :timestamp WHERE user_id = :userId")
    ListenableFuture<Integer> updateLastActive(int userId, LocalDateTime timestamp);

    @Query("DELETE FROM global_statistics WHERE user_id = :userId")
    ListenableFuture<Integer> deleteStatisticsForUser(int userId);

    @Query("SELECT * FROM global_statistics WHERE user_id = :userId LIMIT 1")
    ListenableFuture<GlobalStatistics> getGlobalStatisticsSuspend(int userId);


    // --- SYNC ---
    @Query("SELECT * FROM global_statistics WHERE user_id = :userId LIMIT 1")
    GlobalStatistics getGlobalStatisticsSync(int userId); // НОВЫЙ

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdateGlobalStatisticsSync(GlobalStatistics globalStatistics); // Уже был

    @Query("UPDATE global_statistics SET total_tasks = total_tasks + 1 WHERE user_id = :userId")
    void incrementTotalTasksSync(int userId); // Уже был

    @Query("UPDATE global_statistics SET completed_tasks = completed_tasks + 1 WHERE user_id = :userId")
    void incrementCompletedTasksSync(int userId); // Уже был

    @Query("UPDATE global_statistics SET total_time_spent = total_time_spent + :timeToAdd WHERE user_id = :userId")
    void addTotalTimeSpentSync(int userId, int timeToAdd); // Уже был

    @Query("UPDATE global_statistics SET last_active = :timestamp WHERE user_id = :userId")
    void updateLastActiveSync(int userId, LocalDateTime timestamp); // Уже был
}