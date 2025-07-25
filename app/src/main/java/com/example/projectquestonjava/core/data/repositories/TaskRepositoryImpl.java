package com.example.projectquestonjava.core.data.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.data.dao.TaskDao;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.data.model.relations.TaskWithTags;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskRepositoryImpl implements TaskRepository {

    private static final String TAG = "TaskRepositoryImpl";
    private final TaskDao taskDao;
    private final UserSessionManager userSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public TaskRepositoryImpl(
            TaskDao taskDao,
            UserSessionManager userSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskDao = taskDao;
        this.userSessionManager = userSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    @FunctionalInterface
    private interface UserSpecificOperation<T> {
        ListenableFuture<T> execute(int userId);
    }
    @FunctionalInterface
    private interface UserSpecificOperationLiveData<T> {
        LiveData<T> execute(int userId);
    }


    private <T> ListenableFuture<T> executeWithUserCheck(UserSpecificOperation<T> operation) {
        int userId = userSessionManager.getUserIdSync();
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.warn(TAG, "Operation failed: User not logged in.");
            return Futures.immediateFailedFuture(new IllegalStateException("User not logged in"));
        }
        return operation.execute(userId);
    }

    private <T> LiveData<T> executeWithUserCheckLiveData(UserSpecificOperationLiveData<T> operation, T defaultValue) {
        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "LiveData operation: User not logged in. Returning default.");
                return new LiveData<T>(defaultValue) {};
            }
            return operation.execute(userId);
        });
    }


    @Override
    public ListenableFuture<Long> insertTask(Task task) {
        logger.debug(TAG, "Inserting task: " + task.getTitle());
        return taskDao.insertTask(task); // DAO уже ListenableFuture
    }

    @Override
    public ListenableFuture<Void> updateTask(Task task) {
        logger.debug(TAG, "Updating task: " + task.getId());
        return taskDao.updateTask(task);
    }

    @Override
    public ListenableFuture<Void> updateTaskStatus(long taskId, TaskStatus status) {
        return executeWithUserCheck(userId -> {
            logger.debug(TAG, "Updating status for task " + taskId + " to " + status + " for user " + userId);
            ListenableFuture<Integer> updatedRowsFuture = taskDao.updateTaskStatus(taskId, userId, status, dateTimeUtils.currentUtcDateTime());
            return Futures.transform(updatedRowsFuture, count -> null, ioExecutor);
        });
    }

    @Override
    public LiveData<List<Task>> getUpcomingTasks() {
        return executeWithUserCheckLiveData(
                userId -> taskDao.getUpcomingTasks(userId, dateTimeUtils.currentUtcDateTime()),
                Collections.emptyList()
        );
    }

    @Override
    public LiveData<List<Task>> getAllTasksForUser() {
        return executeWithUserCheckLiveData(taskDao::getAllTasksForUser, Collections.emptyList());
    }

    @Override
    public ListenableFuture<Task> getTaskById(long id) {
        return executeWithUserCheck(userId -> {
            logger.debug(TAG, "Getting task by id " + id + " for user " + userId);
            return taskDao.getTaskById(id, userId);
        });
    }

    @Override
    public ListenableFuture<List<Task>> getAllTasks() {
        return executeWithUserCheck(userId -> {
            // Если в DAO нет suspend getAllTasks(userId), а есть LiveData<List<Task>> getAllTasksForUser(int userId)
            // то этот метод нужно будет либо удалить, либо реализовать через LiveData.get(), что не очень хорошо.

            // Пока заглушка:
            logger.warn(TAG, "getAllTasks (suspend List) is not optimally implemented without a direct DAO method.");
            return Futures.immediateFuture(Collections.emptyList()); // Заглушка
        });
    }

    @Override
    public ListenableFuture<Void> deleteTask(Task task) {
        return deleteTaskById(task.getId());
    }

    @Override
    public ListenableFuture<Void> deleteTaskById(long taskId) {
        return executeWithUserCheck(userId -> {
            logger.debug(TAG, "Deleting task " + taskId + " for user " + userId);
            ListenableFuture<Integer> deletedRowsFuture = taskDao.deleteTaskById(taskId, userId);
            return Futures.transformAsync(deletedRowsFuture, count -> {
                if (count == 0) {
                    logger.warn(TAG, "Task " + taskId + " not found or access denied for user " + userId + " during delete.");
                } else {
                    logger.info(TAG, "Task " + taskId + " deleted successfully for user " + userId);
                }
                return Futures.immediateFuture(null);
            }, ioExecutor);
        });
    }

    @Override
    public LiveData<List<Task>> getTasksForWorkspace(long workspaceId) {
        return executeWithUserCheckLiveData(
                userId -> taskDao.getTasksForWorkspace(workspaceId, userId),
                Collections.emptyList()
        );
    }

    @Override
    public ListenableFuture<TaskWithTags> getTaskWithTagsById(long taskId) {
        return executeWithUserCheck(userId -> {
            logger.debug(TAG, "Getting task with tags by id " + taskId + " for user " + userId);
            return taskDao.getTaskWithTagsById(taskId, userId);
        });
    }

    // --- РЕАЛИЗАЦИИ SYNC МЕТОДОВ ---
    @Override
    public long insertTaskSync(Task task) {
        logger.debug(TAG, "Inserting task SYNC: " + task.getTitle());
        return taskDao.insertTaskSync(task);
    }

    @Override
    public Task getTaskByIdSync(long id) {
        int userId = userSessionManager.getUserIdSync();
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.warn(TAG, "Cannot get task SYNC by id " + id + ": User not logged in.");
            return null; // или бросить исключение
        }
        logger.debug(TAG, "Getting task SYNC by id " + id + " for user " + userId);
        return taskDao.getTaskByIdSync(id, userId);
    }

    @Override
    public void updateTaskSync(Task task) {
        int userId = userSessionManager.getUserIdSync();
        // Дополнительная проверка, что обновляется задача текущего пользователя
        if (userId == UserSessionManager.NO_USER_ID || task.getUserId() != userId) {
            logger.error(TAG, "Cannot update task SYNC: User ID mismatch or not logged in. Task User: " + task.getUserId() + ", Current User: " + userId);
            return;
        }
        logger.debug(TAG, "Updating task SYNC: " + task.getId());
        int updatedRows = taskDao.updateTaskSync(task);
        if (updatedRows == 0) {
            logger.warn(TAG, "Task SYNC update affected 0 rows for taskId=" + task.getId());
        }
    }

    @Override
    public void updateTaskStatusSync(long taskId, int userId, TaskStatus status, LocalDateTime updatedAt) {
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.error(TAG, "Cannot update task status SYNC: Invalid userId (-1). TaskId: " + taskId);
            return;
        }
        logger.debug(TAG, "Updating task status SYNC for taskId=" + taskId + ", userId=" + userId + " to " + status);
        int updatedRows = taskDao.updateTaskStatusSync(taskId, userId, status, updatedAt);
        if (updatedRows == 0) {
            logger.warn(TAG, "Task status SYNC update affected 0 rows for taskId=" + taskId + ", userId=" + userId);
        }
    }

    @Override
    public TaskWithTags getTaskWithTagsByIdSync(long taskId, int userId) {
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.warn(TAG, "Cannot get task with tags SYNC for taskId=" + taskId + ": User not logged in (userId was -1).");
        }
        logger.debug(TAG, "Getting task with tags SYNC for taskId=" + taskId + ", userId=" + userId);
        return taskDao.getTaskWithTagsByIdSync(taskId, userId);
    }
}