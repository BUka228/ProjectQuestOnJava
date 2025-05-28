package com.example.projectquestonjava.feature.statistics.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.statistics.data.dao.GlobalStatisticsDao;
import com.example.projectquestonjava.feature.statistics.data.model.GlobalStatistics;
import com.example.projectquestonjava.feature.statistics.domain.repository.GlobalStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GlobalStatisticsRepositoryImpl implements GlobalStatisticsRepository {

    private static final String TAG = "GlobalStatsRepoImpl";
    private final GlobalStatisticsDao globalStatisticsDao;
    private final UserSessionManager userSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public GlobalStatisticsRepositoryImpl(
            GlobalStatisticsDao globalStatisticsDao,
            UserSessionManager userSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.globalStatisticsDao = globalStatisticsDao;
        this.userSessionManager = userSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    private <T> ListenableFuture<T> executeWithUser(UserSpecificOperation<T> function) {
        int userId = userSessionManager.getUserIdSync();
        if (userId == UserSessionManager.NO_USER_ID) {
            return Futures.immediateFailedFuture(new IllegalStateException("User not logged in"));
        }
        return function.apply(userId);
    }

    private <T> LiveData<T> executeWithUserLiveData(UserSpecificLiveDataOperation<T> function, T defaultValue) {
        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                return new LiveData<T>(defaultValue) {};
            }
            return function.apply(userId);
        });
    }

    @Override
    public LiveData<GlobalStatistics> getGlobalStatisticsFlow() {
        logger.debug(TAG, "Getting global statistics LiveData for current user");
        return executeWithUserLiveData(globalStatisticsDao::getGlobalStatistics, null);
    }

    @Override
    public ListenableFuture<GlobalStatistics> getGlobalStatisticsSuspend() {
        return executeWithUser(userId -> {
            logger.debug(TAG, "Getting global statistics suspend for userId=" + userId);
            return globalStatisticsDao.getGlobalStatisticsSuspend(userId);
        });
    }

    @Override
    public ListenableFuture<Void> incrementTotalTasks() {
        return executeWithUser(userId -> {
            ListenableFuture<Integer> future = globalStatisticsDao.incrementTotalTasks(userId);
            logger.debug(TAG, "Incremented total tasks for user " + userId);
            return Futures.transform(future, count -> null, MoreExecutors.directExecutor());
        });
    }

    @Override
    public ListenableFuture<Void> incrementCompletedTasks() {
        return executeWithUser(userId -> {
            ListenableFuture<Integer> future = globalStatisticsDao.incrementCompletedTasks(userId);
            logger.debug(TAG, "Incremented completed tasks for user " + userId);
            return Futures.transform(future, count -> null, MoreExecutors.directExecutor());
        });
    }

    @Override
    public ListenableFuture<Void> incrementTotalWorkspaces() {
        return executeWithUser(userId -> {
            ListenableFuture<Integer> future = globalStatisticsDao.incrementTotalWorkspaces(userId);
            logger.debug(TAG, "Incremented total workspaces for user " + userId);
            return Futures.transform(future, count -> null, MoreExecutors.directExecutor());
        });
    }

    @Override
    public ListenableFuture<Void> addTotalTimeSpent(int timeToAdd) {
        if (timeToAdd <= 0) return Futures.immediateFuture(null);
        return executeWithUser(userId -> {
            ListenableFuture<Integer> future = globalStatisticsDao.addTotalTimeSpent(userId, timeToAdd);
            logger.debug(TAG, "Added " + timeToAdd + " to total time spent for user " + userId);
            return Futures.transform(future, count -> null, MoreExecutors.directExecutor());
        });
    }

    @Override
    public ListenableFuture<Void> updateLastActive() {
        return executeWithUser(userId -> {
            ListenableFuture<Integer> future = globalStatisticsDao.updateLastActive(userId, dateTimeUtils.currentUtcDateTime());
            logger.debug(TAG, "Updated last active time for user " + userId);
            return Futures.transform(future, count -> null, MoreExecutors.directExecutor());
        });
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

    // --- РЕАЛИЗАЦИИ SYNC МЕТОДОВ ---
    @Override
    public void incrementTotalTasksSync() {
        int userId = userSessionManager.getUserIdSync();
        if (userId != UserSessionManager.NO_USER_ID) {
            logger.debug(TAG, "SYNC Incrementing total tasks for user " + userId);
            globalStatisticsDao.incrementTotalTasksSync(userId);
        } else {
            logger.warn(TAG, "SYNC Cannot increment total tasks: User not logged in.");
        }
    }

    @Override
    public void insertOrUpdateGlobalStatisticsSync(GlobalStatistics globalStatistics) {
        logger.debug(TAG, "SYNC Inserting/Updating global statistics for userId=" + globalStatistics.getUserId());
        globalStatisticsDao.insertOrUpdateGlobalStatisticsSync(globalStatistics);
    }

    @Override
    public void incrementCompletedTasksSync() {
        int userId = userSessionManager.getUserIdSync();
        if (userId != UserSessionManager.NO_USER_ID) {
            logger.debug(TAG, "SYNC Incrementing completed tasks for user " + userId);
            globalStatisticsDao.incrementCompletedTasksSync(userId);
        } else {
            logger.warn(TAG, "SYNC Cannot increment completed tasks: User not logged in.");
        }
    }

    @Override
    public void addTotalTimeSpentSync(int timeToAdd) {
        if (timeToAdd <= 0) return;
        int userId = userSessionManager.getUserIdSync();
        if (userId != UserSessionManager.NO_USER_ID) {
            logger.debug(TAG, "SYNC Adding " + timeToAdd + " to total time spent for user " + userId);
            globalStatisticsDao.addTotalTimeSpentSync(userId, timeToAdd);
        } else {
            logger.warn(TAG, "SYNC Cannot add total time spent: User not logged in.");
        }
    }

    @Override
    public void updateLastActiveSync() {
        int userId = userSessionManager.getUserIdSync();
        if (userId != UserSessionManager.NO_USER_ID) {
            logger.debug(TAG, "SYNC Updating last active for user " + userId);
            globalStatisticsDao.updateLastActiveSync(userId, dateTimeUtils.currentUtcDateTime());
        } else {
            logger.warn(TAG, "SYNC Cannot update last active: User not logged in.");
        }
    }
}