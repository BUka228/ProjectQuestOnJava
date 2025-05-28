package com.example.projectquestonjava.approach.calendar.data.repositories;

import androidx.lifecycle.LiveData;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.data.dao.CalendarTaskDao;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarParamsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor; // Нужен Executor
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CalendarParamsRepositoryImpl implements CalendarParamsRepository {

    private static final String TAG = "CalendarParamsRepo";
    private final CalendarTaskDao calendarTaskDao;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public CalendarParamsRepositoryImpl(
            CalendarTaskDao calendarTaskDao,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.calendarTaskDao = calendarTaskDao;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    @Override
    public ListenableFuture<Long> insertParams(CalendarParams params) {
        logger.debug(TAG, "Inserting CalendarParams for taskId=" + params.getTaskId());

        return Futures.catchingAsync(
                calendarTaskDao.insertCalendarParams(params),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error inserting CalendarParams for taskId=" + params.getTaskId(), e);
                    throw new RuntimeException("Insert failed", e); // Пробрасываем для ListenableFuture
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> updateParams(CalendarParams params) {
        logger.debug(TAG, "Updating CalendarParams for taskId=" + params.getTaskId());
        return Futures.catchingAsync(
                calendarTaskDao.updateCalendarParams(params),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error updating CalendarParams for taskId=" + params.getTaskId(), e);
                    throw new RuntimeException("Update failed", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> deleteParams(CalendarParams params) {
        logger.debug(TAG, "Deleting CalendarParams for taskId=" + params.getTaskId());
        return Futures.catchingAsync(
                calendarTaskDao.deleteCalendarParams(params),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error deleting CalendarParams for taskId=" + params.getTaskId(), e);
                    throw new RuntimeException("Delete failed", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<CalendarParams> getParamsByTaskId(long taskId) {
        logger.debug(TAG, "Getting CalendarParams for taskId=" + taskId);
        return Futures.catchingAsync(
                calendarTaskDao.getCalendarParamsByTaskId(taskId),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting CalendarParams for taskId=" + taskId, e);
                    return null;
                },
                ioExecutor
        );
    }

    @Override
    public LiveData<List<CalendarParams>> getParamsForTasksFlow(List<Long> taskIds) {
        logger.debug(TAG, "Getting CalendarParams LiveData for taskIds=" + taskIds);
        if (taskIds == null || taskIds.isEmpty()) {

            return new LiveData<>(Collections.emptyList()) {
            };
        }
        return calendarTaskDao.getParamsForTasks(taskIds);

    }


    // --- РЕАЛИЗАЦИИ SYNC МЕТОДОВ ---
    @Override
    public long insertParamsSync(CalendarParams params) {
        logger.debug(TAG, "SYNC Inserting CalendarParams for taskId=" + params.getTaskId());
        try {
            return calendarTaskDao.insertCalendarParamsSync(params);
        } catch (Exception e) {
            logger.error(TAG, "Error SYNC inserting CalendarParams for taskId=" + params.getTaskId(), e);
            throw new RuntimeException("Sync insert failed", e); // Пробрасываем, чтобы транзакция откатилась
        }
    }

    @Override
    public void updateParamsSync(CalendarParams params) {
        logger.debug(TAG, "SYNC Updating CalendarParams for taskId=" + params.getTaskId());
        try {
            calendarTaskDao.updateCalendarParamsSync(params);
        } catch (Exception e) {
            logger.error(TAG, "Error SYNC updating CalendarParams for taskId=" + params.getTaskId(), e);
            throw new RuntimeException("Sync update failed", e);
        }
    }

    @Override
    public CalendarParams getParamsByTaskIdSync(long taskId) {
        logger.debug(TAG, "SYNC Getting CalendarParams for taskId=" + taskId);
        try {
            return calendarTaskDao.getCalendarParamsByTaskIdSync(taskId);
        } catch (Exception e) {
            logger.error(TAG, "Error SYNC getting CalendarParams for taskId=" + taskId, e);
            return null; // Или throw, если null недопустим
        }
    }
}