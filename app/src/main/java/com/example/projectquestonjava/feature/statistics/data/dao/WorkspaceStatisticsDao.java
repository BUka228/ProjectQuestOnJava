package com.example.projectquestonjava.feature.statistics.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.projectquestonjava.feature.statistics.data.model.WorkspaceStatistics;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface WorkspaceStatisticsDao {
    @Query("SELECT ws.* FROM workspace_statistics ws JOIN Workspace w ON ws.workspace_id = w.id WHERE ws.workspace_id = :workspaceId AND w.user_id = :userId")
    LiveData<WorkspaceStatistics> getWorkspaceStatistics(long workspaceId, int userId);

    @Query("SELECT ws.* FROM workspace_statistics ws JOIN Workspace w ON ws.workspace_id = w.id WHERE w.user_id = :userId")
    LiveData<List<WorkspaceStatistics>> getAllWorkspaceStatisticsForUser(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insertOrUpdateWorkspaceStatistics(WorkspaceStatistics statistics);

    @Query("UPDATE workspace_statistics SET total_tasks = total_tasks + 1 WHERE workspace_id = :workspaceId AND EXISTS (SELECT 1 FROM Workspace w WHERE w.id = :workspaceId AND w.user_id = :userId)")
    ListenableFuture<Integer> incrementTotalTasks(long workspaceId, int userId);

    @Query("UPDATE workspace_statistics SET completed_tasks = completed_tasks + 1 WHERE workspace_id = :workspaceId AND EXISTS (SELECT 1 FROM Workspace w WHERE w.id = :workspaceId AND w.user_id = :userId)")
    ListenableFuture<Integer> incrementCompletedTasks(long workspaceId, int userId);

    @Query("DELETE FROM workspace_statistics WHERE workspace_id = :workspaceId")
    ListenableFuture<Integer> deleteStatisticsForWorkspace(long workspaceId);
}