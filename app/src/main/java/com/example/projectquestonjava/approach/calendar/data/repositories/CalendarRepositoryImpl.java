package com.example.projectquestonjava.approach.calendar.data.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.data.dao.TaskDao;
import com.example.projectquestonjava.core.data.model.relations.TaskWithTags;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskWithTagsAndPomodoro;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarParamsRepository;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarRepository;
import com.example.projectquestonjava.feature.statistics.data.model.TaskStatistics;
import com.example.projectquestonjava.feature.statistics.domain.repository.TaskStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CalendarRepositoryImpl implements CalendarRepository {

    private static final String TAG = "CalendarRepositoryImpl";
    private final TaskDao taskDao;
    private final CalendarParamsRepository calendarParamsRepository;
    private final TaskStatisticsRepository taskStatisticsRepository;
    private final UserSessionManager userSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public CalendarRepositoryImpl(
            TaskDao taskDao,
            CalendarParamsRepository calendarParamsRepository,
            TaskStatisticsRepository taskStatisticsRepository,
            UserSessionManager userSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskDao = taskDao;
        this.calendarParamsRepository = calendarParamsRepository;
        this.taskStatisticsRepository = taskStatisticsRepository;
        this.userSessionManager = userSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    @Override
    public LiveData<List<CalendarTaskWithTagsAndPomodoro>> getTasksForDay(long workspaceId, LocalDateTime day) {
        DateTimeUtils.Pair<Long, Long> boundaries = dateTimeUtils.calculateUtcDayBoundariesEpochSeconds(day);
        logger.debug(TAG, "Getting tasks for day: workspace=" + workspaceId + ", start=" + boundaries.first() + ", end=" + boundaries.second());

        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "No user for getTasksForDay.");
                return new LiveData<>(Collections.emptyList()) {
                };
            }
            LiveData<List<TaskWithTags>> tasksWithTagsLiveData = taskDao.getTasksWithTagsForWorkspaceInDateRange(workspaceId, userId, boundaries.first(), boundaries.second());
            return combineWithStatsAndParamsLiveData(tasksWithTagsLiveData);
        });
    }

    @Override
    public LiveData<List<CalendarTaskWithTagsAndPomodoro>> getTasksForMonth(long workspaceId, LocalDate startTimestamp) {
        DateTimeUtils.Pair<Long, Long> boundaries = dateTimeUtils.calculateUtcMonthBoundariesEpochSeconds(startTimestamp);
        logger.debug(TAG, "Getting tasks for month: workspace=" + workspaceId + ", start=" + boundaries.first() + ", end=" + boundaries.second());

        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "No user for getTasksForMonth.");
                return new LiveData<>(Collections.emptyList()) {
                };
            }
            LiveData<List<TaskWithTags>> tasksWithTagsLiveData = taskDao.getTasksWithTagsForWorkspaceInDateRange(workspaceId, userId, boundaries.first(), boundaries.second());
            return combineWithStatsAndParamsLiveData(tasksWithTagsLiveData);
        });
    }

    @Override
    public ListenableFuture<CalendarTaskWithTagsAndPomodoro> getTaskWithTagsAndPomodoroById(long workspaceId, long taskId) {
        int userId = userSessionManager.getUserIdSync();
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.warn(TAG, "Cannot get task " + taskId + ", no user logged in.");
            return Futures.immediateFuture(null);
        }
        logger.debug(TAG, "Getting task details for taskId=" + taskId + ", userId=" + userId);

        ListenableFuture<TaskWithTags> taskWithTagsFuture = taskDao.getTaskWithTagsById(taskId, userId);

        return Futures.transformAsync(taskWithTagsFuture, taskWithTags -> {
            if (taskWithTags == null) {
                logger.warn(TAG, "Task " + taskId + " not found for user " + userId + ".");
                return Futures.immediateFuture(null);
            }
            if (taskWithTags.getTask().getWorkspaceId() != workspaceId && workspaceId != -1L) {
                logger.warn(TAG, "Task " + taskId + " belongs to workspace " + taskWithTags.getTask().getWorkspaceId() + ", but requested for " + workspaceId + ".");
                return Futures.immediateFuture(null);
            }

            ListenableFuture<TaskStatistics> statsFuture = taskStatisticsRepository.getStatisticsForTask(taskId);
            ListenableFuture<CalendarParams> paramsFuture = calendarParamsRepository.getParamsByTaskId(taskId);

            ListenableFuture<List<Object>> combinedFutures = Futures.allAsList(statsFuture, paramsFuture);

            return Futures.transform(combinedFutures, results -> {
                TaskStatistics stats = (TaskStatistics) results.get(0);
                CalendarParams params = (CalendarParams) results.get(1);

                if (params == null) {
                    logger.error(TAG, "CRITICAL: CalendarParams missing for existing task " + taskWithTags.getTask().getId() + ".");
                    return null;
                }
                int pomodoroCount = (stats != null) ? stats.getCompletedPomodoroFocusSessions() : 0;
                return new CalendarTaskWithTagsAndPomodoro(taskWithTags.getTask(), params, taskWithTags.getTags(), pomodoroCount);
            }, MoreExecutors.directExecutor()); // Используем directExecutor, т.к. предыдущие future уже на ioExecutor

        }, ioExecutor); // taskDao.getTaskWithTagsById выполняется на ioExecutor
    }

    private LiveData<List<CalendarTaskWithTagsAndPomodoro>> combineWithStatsAndParamsLiveData(LiveData<List<TaskWithTags>> tasksWithTagsLiveData) {
        MediatorLiveData<List<CalendarTaskWithTagsAndPomodoro>> resultLiveData = new MediatorLiveData<>();

        resultLiveData.addSource(tasksWithTagsLiveData, tasksWithTags -> {
            if (tasksWithTags == null || tasksWithTags.isEmpty()) {
                resultLiveData.setValue(Collections.emptyList());
                return;
            }
            List<Long> taskIds = tasksWithTags.stream().map(twt -> twt.getTask().getId()).collect(Collectors.toList());
            logger.debug(TAG, "Combining details for tasks: " + taskIds);

            LiveData<List<TaskStatistics>> statsLiveData = taskStatisticsRepository.getStatisticsForTasksFlow(taskIds);
            LiveData<List<CalendarParams>> paramsLiveData = calendarParamsRepository.getParamsForTasksFlow(taskIds);

            // Объединяем LiveData
            MediatorLiveData<CombinedData> tempMediator = new MediatorLiveData<>();
            final List<TaskStatistics>[] statsHolder = new List[1]; // Массив для хранения значения, т.к. лямбда требует final
            final List<CalendarParams>[] paramsHolder = new List[1];

            tempMediator.addSource(statsLiveData, stats -> {
                statsHolder[0] = stats;
                if (paramsHolder[0] != null) {
                    tempMediator.setValue(new CombinedData(statsHolder[0], paramsHolder[0]));
                }
            });
            tempMediator.addSource(paramsLiveData, params -> {
                paramsHolder[0] = params;
                if (statsHolder[0] != null) {
                    tempMediator.setValue(new CombinedData(statsHolder[0], paramsHolder[0]));
                }
            });

            // Этот addSource будет внешним для tempMediator, чтобы отписываться от statsLiveData и paramsLiveData
            resultLiveData.addSource(tempMediator, combinedData -> {
                if (combinedData == null || combinedData.stats == null || combinedData.params == null) {
                    resultLiveData.setValue(Collections.emptyList()); // или предыдущее значение, если нужно
                    return;
                }

                Map<Long, TaskStatistics> statsMap = combinedData.stats.stream().collect(Collectors.toMap(TaskStatistics::getTaskId, ts -> ts));
                Map<Long, CalendarParams> paramsMap = combinedData.params.stream().collect(Collectors.toMap(CalendarParams::getTaskId, cp -> cp));

                List<CalendarTaskWithTagsAndPomodoro> resultList = new ArrayList<>();
                for (TaskWithTags taskWithTagsItem : tasksWithTags) {
                    CalendarParams params = paramsMap.get(taskWithTagsItem.getTask().getId());
                    if (params == null) {
                        logger.warn(TAG, "CalendarParams missing for task " + taskWithTagsItem.getTask().getId() + " during combine. Skipping.");
                        continue;
                    }
                    TaskStatistics stats = statsMap.get(taskWithTagsItem.getTask().getId());
                    int pomodoroCount = (stats != null) ? stats.getCompletedPomodoroFocusSessions() : 0;
                    resultList.add(new CalendarTaskWithTagsAndPomodoro(taskWithTagsItem.getTask(), params, taskWithTagsItem.getTags(), pomodoroCount));
                }
                logger.debug(TAG, "Combine result size for tasks " + taskIds + ": " + resultList.size());
                resultLiveData.setValue(resultList);
            });
        });
        return resultLiveData;
    }

    // Вспомогательный класс для MediatorLiveData
    private static class CombinedData {
        final List<TaskStatistics> stats;
        final List<CalendarParams> params;
        CombinedData(List<TaskStatistics> stats, List<CalendarParams> params) {
            this.stats = stats;
            this.params = params;
        }
    }
}