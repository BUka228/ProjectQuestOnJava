package com.example.projectquestonjava.core.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.core.data.model.core.Workspace;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface WorkspaceDao {
    // --- ASYNC / LiveData ---
    @Query("SELECT * FROM workspace WHERE user_id = :userId")
    LiveData<List<Workspace>> getAllWorkspaces(int userId);

    @Query("SELECT * FROM workspace WHERE id = :id AND user_id = :userId")
    LiveData<Workspace> getWorkspaceById(long id, int userId);

    @Insert
    ListenableFuture<Long> insertWorkspace(Workspace workspace);

    @Update
    ListenableFuture<Integer> updateWorkspace(Workspace workspace);

    @Delete
    ListenableFuture<Integer> deleteWorkspace(Workspace workspace);

    @Query("DELETE FROM workspace WHERE id = :workspaceId AND user_id = :userId")
    ListenableFuture<Integer> deleteWorkspaceById(long workspaceId, int userId);

    @Query("DELETE FROM workspace WHERE user_id = :userId")
    ListenableFuture<Integer> deleteWorkspacesForUser(int userId);

    // --- SYNC ---
    @Query("SELECT * FROM workspace WHERE user_id = :userId")
    List<Workspace> getAllWorkspacesSync(int userId);

    @Query("SELECT * FROM workspace WHERE id = :id AND user_id = :userId")
    Workspace getWorkspaceByIdSync(long id, int userId);

    @Insert
    long insertWorkspaceSync(Workspace workspace);

    @Update
    int updateWorkspaceSync(Workspace workspace);

    @Delete
    int deleteWorkspaceSync(Workspace workspace);

    @Query("DELETE FROM workspace WHERE id = :workspaceId AND user_id = :userId")
    int deleteWorkspaceByIdSync(long workspaceId, int userId);
}