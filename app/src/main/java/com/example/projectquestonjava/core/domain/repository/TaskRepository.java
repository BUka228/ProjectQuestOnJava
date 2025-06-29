package com.example.projectquestonjava.core.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.data.model.relations.TaskWithTags;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository {
    // --- ASYNC / LiveData ---
    ListenableFuture<Long> insertTask(Task task);
    ListenableFuture<Void> updateTask(Task task);
    ListenableFuture<Void> updateTaskStatus(long taskId, TaskStatus status);
    LiveData<List<Task>> getUpcomingTasks();
    LiveData<List<Task>> getAllTasksForUser();
    ListenableFuture<Task> getTaskById(long id);
    ListenableFuture<List<Task>> getAllTasks();
    ListenableFuture<Void> deleteTask(Task task);
    LiveData<List<Task>> getTasksForWorkspace(long workspaceId);
    ListenableFuture<TaskWithTags> getTaskWithTagsById(long taskId);
    ListenableFuture<Void> deleteTaskById(long taskId);

    // --- SYNC ---
    long insertTaskSync(Task task); // Уже был
    Task getTaskByIdSync(long id);
    void updateTaskSync(Task task);
    void updateTaskStatusSync(long taskId, int userId, TaskStatus status, LocalDateTime updatedAt);
    TaskWithTags getTaskWithTagsByIdSync(long taskId, int userId);
}