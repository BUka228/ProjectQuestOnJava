package com.example.projectquestonjava.core.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.data.model.relations.TaskWithTags;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface TaskDao {

    @Transaction
    @Query("SELECT * FROM task WHERE id = :taskId AND user_id = :userId")
    ListenableFuture<TaskWithTags> getTaskWithTagsById(long taskId, int userId);

    @Transaction
    @Query("SELECT * FROM task WHERE workspace_id = :workspaceId AND user_id = :userId AND due_date BETWEEN :startTime AND :endTime")
    LiveData<List<TaskWithTags>> getTasksWithTagsForWorkspaceInDateRange(long workspaceId, int userId, long startTime, long endTime);

    @Query("SELECT * FROM task WHERE workspace_id = :workspaceId AND user_id = :userId")
    LiveData<List<Task>> getTasksForWorkspace(long workspaceId, int userId);

    @Query("SELECT * FROM task WHERE user_id = :userId")
    LiveData<List<Task>> getAllTasksForUser(int userId);

    @Query("SELECT * FROM task WHERE id = :id AND user_id = :userId")
    ListenableFuture<Task> getTaskById(long id, int userId);

    @Query("UPDATE task SET status = :newStatus, updated_at = :updatedAt WHERE id = :taskId AND user_id = :userId")
    ListenableFuture<Integer> updateTaskStatus(long taskId, int userId, TaskStatus newStatus, LocalDateTime updatedAt);

    @Query("SELECT * FROM task WHERE user_id = :userId AND due_date >= :from ORDER BY due_date ASC LIMIT 5")
    LiveData<List<Task>> getUpcomingTasks(int userId, LocalDateTime from);

    @Insert
    ListenableFuture<Long> insertTask(Task task);

    @Update
    ListenableFuture<Void> updateTask(Task task);

    @Delete
    ListenableFuture<Void> deleteTask(Task task);

    @Query("DELETE FROM task WHERE id = :taskId AND user_id = :userId")
    ListenableFuture<Integer> deleteTaskById(long taskId, int userId);

    @Query("DELETE FROM task WHERE user_id = :userId")
    ListenableFuture<Void> deleteTasksForUser(int userId);

    @Query("DELETE FROM task WHERE workspace_id = :workspaceId AND user_id = :userId")
    ListenableFuture<Void> deleteTasksForWorkspace(long workspaceId, int userId);


    @Insert
    long insertTaskSync(Task task);

    @Transaction
    @Query("SELECT * FROM task WHERE id = :taskId AND user_id = :userId")
    TaskWithTags getTaskWithTagsByIdSync(long taskId, int userId);

    @Query("SELECT * FROM task WHERE id = :id AND user_id = :userId")
    Task getTaskByIdSync(long id, int userId);

    @Update
    int updateTaskSync(Task task);

    @Query("UPDATE task SET status = :newStatus, updated_at = :updatedAt WHERE id = :taskId AND user_id = :userId")
    int updateTaskStatusSync(long taskId, int userId, TaskStatus newStatus, LocalDateTime updatedAt);
}