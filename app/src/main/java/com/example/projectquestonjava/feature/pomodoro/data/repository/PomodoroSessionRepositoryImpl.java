package com.example.projectquestonjava.feature.pomodoro.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.data.dao.PomodoroSessionDao;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSessionRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PomodoroSessionRepositoryImpl implements PomodoroSessionRepository {

    private static final String TAG = "PomodoroSessionRepo";
    private final PomodoroSessionDao pomodoroSessionDao;
    private final UserSessionManager userSessionManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public PomodoroSessionRepositoryImpl(
            PomodoroSessionDao pomodoroSessionDao,
            UserSessionManager userSessionManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.pomodoroSessionDao = pomodoroSessionDao;
        this.userSessionManager = userSessionManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    // Вспомогательный метод для выполнения операций с проверкой userId
    private <T> ListenableFuture<T> executeWithUser(UserSpecificOperation<T> operation) {
        int userId = userSessionManager.getUserIdSync(); // Блокирующий, но предполагается вызов из ioExecutor
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.warn(TAG, "Operation failed: User not logged in.");
            return Futures.immediateFailedFuture(new IllegalStateException("User not logged in"));
        }
        return operation.apply(userId);
    }

    // Вспомогательный метод для LiveData операций с проверкой userId
    private <T> LiveData<T> executeWithUserLiveData(UserSpecificLiveDataOperation<T> operation, T defaultValue) {
        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "LiveData operation: User not logged in. Returning default.");
                return new LiveData<T>(defaultValue) {};
            }
            return operation.apply(userId);
        });
    }


    @Override
    public LiveData<List<PomodoroSession>> getSessionsForTask(long taskId) {
        logger.debug(TAG, "Requesting sessions LiveData for task ID=" + taskId);
        return executeWithUserLiveData(userId -> pomodoroSessionDao.getSessionsForTask(taskId, userId), Collections.emptyList());
    }

    @Override
    public ListenableFuture<Long> insertSession(PomodoroSession session) {
        return executeWithUser(userId -> {
            if (session.getUserId() != userId) {
                logger.error(TAG, "Insert session attempt for invalid user. Session User: " + session.getUserId() + ", Current User: " + userId);
                return Futures.immediateFailedFuture(new IllegalStateException("Invalid user context for inserting session"));
            }
            logger.info(TAG, "Inserting new session: taskId=" + session.getTaskId() + ", userId=" + session.getUserId());
            return Futures.catchingAsync(
                    pomodoroSessionDao.insert(session),
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Error inserting session", e);
                        throw new RuntimeException("Insert session failed", e);
                    },
                    ioExecutor);
        });
    }

    @Override
    public ListenableFuture<Void> updateSession(PomodoroSession session) {
        return executeWithUser(userId -> {
            if (session.getUserId() != userId) {
                logger.error(TAG, "Update session attempt for invalid user. Session User: " + session.getUserId() + ", Current User: " + userId);
                return Futures.immediateFailedFuture(new IllegalStateException("Invalid user context for updating session"));
            }
            logger.info(TAG, "Updating session: sessionId=" + session.getId());
            ListenableFuture<Integer> updateFuture = pomodoroSessionDao.update(session);
            return Futures.transform(updateFuture, count -> null, MoreExecutors.directExecutor());
        });
    }

    @Override
    public LiveData<Integer> getCompletedSessionsCount(long taskId) {
        logger.debug(TAG, "Requesting completed sessions count LiveData for task ID=" + taskId);
        return executeWithUserLiveData(userId -> pomodoroSessionDao.getCompletedFocusSessionsCount(taskId, userId), 0);
    }

    @Override
    public ListenableFuture<PomodoroSession> getLatestSession(long taskId) {
        return executeWithUser(userId -> {
            logger.debug(TAG, "Requesting latest session for task ID=" + taskId + ", userId=" + userId);
            return Futures.catchingAsync(
                    pomodoroSessionDao.getLatestSessionForTask(taskId, userId),
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Error getting latest session for task " + taskId, e);
                        return null; // Или throw
                    },
                    ioExecutor);
        });
    }

    @Override
    public LiveData<List<PomodoroSession>> getAllSessionsForUserFlow() {
        logger.debug(TAG, "Requesting all sessions LiveData for current user");
        return executeWithUserLiveData(pomodoroSessionDao::getAllSessionsForUser, Collections.emptyList());
    }

    @Override
    public ListenableFuture<List<PomodoroSession>> getSessionsInPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        return executeWithUser(userId -> {
            logger.debug(TAG, "Getting sessions for period: " + startTime + " - " + endTime + " (userId=" + userId + ")");
            return Futures.catchingAsync(
                    pomodoroSessionDao.getSessionsInPeriod(userId, startTime, endTime),
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Error getting sessions for period", e);
                        return null;
                    },
                    ioExecutor);
        });
    }

    @Override
    public ListenableFuture<PomodoroSession> getSessionById(long sessionId) {
        logger.debug(TAG, "Getting PomodoroSession by id=" + sessionId);
        // Этот метод не зависит от текущего пользователя, так как ищет по глобальному ID сессии
        return Futures.catchingAsync(
                pomodoroSessionDao.getSessionById(sessionId),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting PomodoroSession by id=" + sessionId, e);
                    return null;
                },
                ioExecutor);
    }

    // Вспомогательные функциональные интерфейсы
    @FunctionalInterface
    private interface UserSpecificOperation<T> {
        ListenableFuture<T> apply(int userId);
    }

    @FunctionalInterface
    private interface UserSpecificLiveDataOperation<T> {
        LiveData<T> apply(int userId);
    }
}