package com.example.projectquestonjava.feature.statistics.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.projectquestonjava.feature.statistics.data.model.TaskHistory;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface TaskHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(TaskHistory taskHistory);

    @Query("SELECT * FROM task_history WHERE task_id = :taskId AND user_id = :userId ORDER BY changed_at DESC")
    LiveData<List<TaskHistory>> getHistoryByTaskId(long taskId, int userId);

    @Query("SELECT * FROM task_history WHERE user_id = :userId ORDER BY changed_at DESC")
    LiveData<List<TaskHistory>> getAllHistoryForUser(int userId);

    @Query("DELETE FROM task_history WHERE task_id = :taskId AND user_id = :userId")
    ListenableFuture<Integer> deleteHistoryForTask(long taskId, int userId);

    @Query("DELETE FROM task_history WHERE user_id = :userId")
    ListenableFuture<Integer> deleteHistoryForUser(int userId);
}