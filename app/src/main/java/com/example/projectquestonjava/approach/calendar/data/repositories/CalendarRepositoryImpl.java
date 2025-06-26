package com.example.projectquestonjava.approach.calendar.data.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        logger.debug(TAG, "getTasksForDay: Requested for workspaceId=" + workspaceId +
                ", localDay=" + day.toLocalDate() +
                ", UTC boundaries (epoch seconds): start=" + boundaries.first() + ", end=" + boundaries.second());

        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "getTasksForDay: No user for getTasksForDay. workspaceId=" + workspaceId + ", day=" + day.toLocalDate());
                MutableLiveData<List<CalendarTaskWithTagsAndPomodoro>> emptyLiveData = new MutableLiveData<>();
                emptyLiveData.setValue(Collections.emptyList());
                return emptyLiveData;
            }
            logger.debug(TAG, "getTasksForDay: Using userId=" + userId + " for workspaceId=" + workspaceId + ", day=" + day.toLocalDate());

            LiveData<List<TaskWithTags>> tasksWithTagsLiveData = taskDao.getTasksWithTagsForWorkspaceInDateRange(workspaceId, userId, boundaries.first(), boundaries.second());

            return Transformations.switchMap(tasksWithTagsLiveData, tasksWithTags -> {
                if (tasksWithTags == null) {
                    logger.warn(TAG, "getTasksForDay: tasksWithTagsLiveData returned null list for userId=" + userId + ", wsId=" + workspaceId + ", day=" + day.toLocalDate());
                } else {
                    logger.info(TAG, "getTasksForDay: tasksWithTagsLiveData returned " + tasksWithTags.size() +
                            " tasks (before combine) for userId=" + userId + ", wsId=" + workspaceId + ", day=" + day.toLocalDate());
                    if (!tasksWithTags.isEmpty()) {
                        for(TaskWithTags twt : tasksWithTags) {
                            logger.debug(TAG, "getTasksForDay: Raw task from DAO: ID=" + twt.getTask().getId() + ", Title='" + twt.getTask().getTitle() + "', DueDate(UTC from DB)=" + twt.getTask().getDueDate());
                        }
                    }
                }
                // Важно: мы передаем ОРИГИНАЛЬНЫЙ tasksWithTagsLiveData в combineWithStatsAndParamsLiveData,
                // а не tasksWithTags, который является результатом одного эмитта.
                // Чтобы избежать проблем с повторной подпиской, лучше, чтобы combineWithStatsAndParamsLiveData
                // напрямую принимал LiveData<List<TaskWithTags>>.
                // Передаем tasksWithTagsLiveData (который уже является LiveData)
                return combineWithStatsAndParamsLiveData(tasksWithTagsLiveData);
            });
        });
    }


    @Override
    public LiveData<List<CalendarTaskWithTagsAndPomodoro>> getTasksForMonth(long workspaceId, LocalDate startTimestamp) {
        DateTimeUtils.Pair<Long, Long> boundaries = dateTimeUtils.calculateUtcMonthBoundariesEpochSeconds(startTimestamp);
        logger.debug(TAG, "getTasksForMonth: Requested for workspaceId=" + workspaceId +
                ", localMonthStart=" + startTimestamp +
                ", UTC boundaries (epoch seconds): start=" + boundaries.first() + ", end=" + boundaries.second());

        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "getTasksForMonth: No user. workspaceId=" + workspaceId + ", month=" + startTimestamp);
                MutableLiveData<List<CalendarTaskWithTagsAndPomodoro>> emptyLiveData = new MutableLiveData<>();
                emptyLiveData.setValue(Collections.emptyList());
                return emptyLiveData;
            }
            logger.debug(TAG, "getTasksForMonth: Using userId=" + userId + " for workspaceId=" + workspaceId + ", month=" + startTimestamp);
            LiveData<List<TaskWithTags>> tasksWithTagsLiveData = taskDao.getTasksWithTagsForWorkspaceInDateRange(workspaceId, userId, boundaries.first(), boundaries.second());

            return Transformations.switchMap(tasksWithTagsLiveData, tasksWithTags -> {
                if (tasksWithTags == null) {
                    logger.warn(TAG, "getTasksForMonth: tasksWithTagsLiveData returned null list for userId=" + userId + ", wsId=" + workspaceId + ", month=" + startTimestamp);
                } else {
                    logger.info(TAG, "getTasksForMonth: tasksWithTagsLiveData returned " + tasksWithTags.size() +
                            " tasks (before combine) for userId=" + userId + ", wsId=" + workspaceId + ", month=" + startTimestamp);
                }
                return combineWithStatsAndParamsLiveData(tasksWithTagsLiveData);
            });
        });
    }

    @Override
    public ListenableFuture<CalendarTaskWithTagsAndPomodoro> getTaskWithTagsAndPomodoroById(long workspaceId, long taskId) {
        return Futures.submitAsync(() -> {
            int userId = userSessionManager.getUserIdSync();
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "getTaskWithTagsAndPomodoroById: Cannot get task " + taskId + ", no user logged in.");
                return Futures.immediateFuture(null);
            }
            logger.debug(TAG, "getTaskWithTagsAndPomodoroById: Getting task details for taskId=" + taskId + ", userId=" + userId + ", workspaceId=" + workspaceId);

            ListenableFuture<TaskWithTags> taskWithTagsFuture = taskDao.getTaskWithTagsById(taskId, userId);

            return Futures.transformAsync(taskWithTagsFuture, taskWithTags -> {
                if (taskWithTags == null) {
                    logger.warn(TAG, "getTaskWithTagsAndPomodoroById: Task " + taskId + " not found for user " + userId + ".");
                    return Futures.immediateFuture(null);
                }
                if (taskWithTags.getTask().getWorkspaceId() != workspaceId && workspaceId != -1L) { // -1L для "любое рабочее пространство"
                    logger.warn(TAG, "getTaskWithTagsAndPomodoroById: Task " + taskId + " belongs to workspace " + taskWithTags.getTask().getWorkspaceId() + ", but requested for " + workspaceId + ".");
                    return Futures.immediateFuture(null);
                }

                ListenableFuture<TaskStatistics> statsFuture = taskStatisticsRepository.getStatisticsForTask(taskId);
                ListenableFuture<CalendarParams> paramsFuture = calendarParamsRepository.getParamsByTaskId(taskId);

                return Futures.whenAllSucceed(statsFuture, paramsFuture)
                        .call(() -> {
                            TaskStatistics stats = Futures.getDone(statsFuture);
                            CalendarParams params = Futures.getDone(paramsFuture);

                            if (params == null) {
                                // Это критично, задача не может существовать без параметров календаря в этой модели
                                logger.error(TAG, "getTaskWithTagsAndPomodoroById: CRITICAL - CalendarParams missing for existing task " + taskWithTags.getTask().getId() + ".");
                                throw new IllegalStateException("CalendarParams missing for task " + taskWithTags.getTask().getId());
                            }

                            int pomodoroCount = (stats != null) ? stats.getCompletedPomodoroFocusSessions() : 0;
                            logger.debug(TAG, "getTaskWithTagsAndPomodoroById: Successfully combined details for task " + taskId);
                            return new CalendarTaskWithTagsAndPomodoro(taskWithTags.getTask(), params, taskWithTags.getTags(), pomodoroCount);
                        }, ioExecutor);
            }, ioExecutor);
        }, ioExecutor);
    }


    private LiveData<List<CalendarTaskWithTagsAndPomodoro>> combineWithStatsAndParamsLiveData(LiveData<List<TaskWithTags>> tasksWithTagsLiveDataInput) {
        MediatorLiveData<List<CalendarTaskWithTagsAndPomodoro>> resultLiveData = new MediatorLiveData<>();

        resultLiveData.addSource(tasksWithTagsLiveDataInput, tasksWithTags -> {
            if (tasksWithTags == null) {
                logger.warn(TAG, "combineWithStatsAndParamsLiveData (outer): Input tasksWithTags is null. Setting empty list to result.");
                resultLiveData.setValue(Collections.emptyList()); // Устанавливаем здесь, если исходные данные null
                return;
            }
            if (tasksWithTags.isEmpty()) {
                logger.debug(TAG, "combineWithStatsAndParamsLiveData (outer): Input tasksWithTags is empty. Setting empty list to result.");
                resultLiveData.setValue(Collections.emptyList()); // И здесь, если исходные данные пустые
                return;
            }
            List<Long> taskIds = tasksWithTags.stream().map(twt -> twt.getTask().getId()).collect(Collectors.toList());
            logger.info(TAG, "combineWithStatsAndParamsLiveData (outer): Received " + taskIds.size() + " tasks. IDs: " + taskIds + ". Preparing to combine details.");

            LiveData<List<TaskStatistics>> statsLiveData = taskStatisticsRepository.getStatisticsForTasksFlow(taskIds);
            LiveData<List<CalendarParams>> paramsLiveData = calendarParamsRepository.getParamsForTasksFlow(taskIds);

            // Передаем tasksWithTags (актуальный список), а не tasksWithTagsLiveDataInput
            CombinedLiveDataSources combinedSources = new CombinedLiveDataSources(tasksWithTags, statsLiveData, paramsLiveData, logger);

            resultLiveData.addSource(combinedSources, data -> { // CombinedLiveDataSources - это LiveData
                if (data != null) {
                    logger.debug(TAG, "combineWithStatsAndParamsLiveData (outer): CombinedData received from CombinedLiveDataSources. Processing...");
                    List<CalendarTaskWithTagsAndPomodoro> processedList = processCombinedData(data.tasksWithTags, data.stats, data.params);
                    resultLiveData.setValue(processedList);
                } else {
                    logger.warn(TAG, "combineWithStatsAndParamsLiveData (outer): CombinedData from CombinedLiveDataSources is null. Setting empty list to result.");
                    resultLiveData.setValue(Collections.emptyList()); // Устанавливаем пустой, если combinedSources вернул null
                }
            });
        });
        return resultLiveData;
    }

    // Вспомогательный класс для объединения источников в MediatorLiveData
    private static class CombinedLiveDataSources extends MediatorLiveData<CombinedData> {
        private List<TaskWithTags> tasksWithTags;
        private List<TaskStatistics> stats;
        private List<CalendarParams> params;
        private final Logger logger;

        CombinedLiveDataSources(List<TaskWithTags> initialTasks,
                                LiveData<List<TaskStatistics>> statsSource,
                                LiveData<List<CalendarParams>> paramsSource,
                                Logger logger) {
            this.tasksWithTags = initialTasks;
            this.logger = logger;
            logger.debug("CombinedLiveDataSources", "Constructor. Initial tasks: " + (initialTasks != null ? initialTasks.size() : "null"));

            addSource(statsSource, s -> {
                logger.debug("CombinedLiveDataSources", "StatsSource updated. Count: " + (s != null ? s.size() : "null"));
                this.stats = s;
                tryUpdateValue();
            });
            addSource(paramsSource, p -> {
                logger.debug("CombinedLiveDataSources", "ParamsSource updated. Count: " + (p != null ? p.size() : "null"));
                this.params = p;
                tryUpdateValue();
            });
            tryUpdateValue(); // Попытка обновить сразу
        }

        private void tryUpdateValue() {
            logger.debug("CombinedLiveDataSources", "tryUpdateValue. Tasks set: " + (tasksWithTags != null) +
                    ", Stats set: " + (stats != null) + ", Params set: " + (params != null));
            if (tasksWithTags != null && stats != null && params != null) {
                logger.info("CombinedLiveDataSources", "All data sources ready. Emitting CombinedData. Tasks: " + tasksWithTags.size() +
                        ", Stats: " + stats.size() + ", Params: " + params.size());
                setValue(new CombinedData(tasksWithTags, stats, params));
            } else {
                logger.debug("CombinedLiveDataSources", "Not all data sources ready yet.");
            }
        }
    }

    private static class CombinedData {
        final List<TaskWithTags> tasksWithTags;
        final List<TaskStatistics> stats;
        final List<CalendarParams> params;
        CombinedData(List<TaskWithTags> tasks, List<TaskStatistics> stats, List<CalendarParams> params) {
            this.tasksWithTags = tasks;
            this.stats = stats;
            this.params = params;
        }
    }

    private List<CalendarTaskWithTagsAndPomodoro> processCombinedData(
            List<TaskWithTags> tasksWithTags,
            List<TaskStatistics> statsList,
            List<CalendarParams> paramsList) {

        logger.debug(TAG, "processCombinedData: Input tasksWithTags: " + (tasksWithTags != null ? tasksWithTags.size() : "null") +
                ", statsList: " + (statsList != null ? statsList.size() : "null") +
                ", paramsList: " + (paramsList != null ? paramsList.size() : "null"));

        if (tasksWithTags == null || tasksWithTags.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, TaskStatistics> statsMap = (statsList != null) ?
                statsList.stream().collect(Collectors.toMap(TaskStatistics::getTaskId, ts -> ts, (ts1, ts2) -> ts1)) : Collections.emptyMap();
        Map<Long, CalendarParams> paramsMap = (paramsList != null) ?
                paramsList.stream().collect(Collectors.toMap(CalendarParams::getTaskId, cp -> cp, (cp1, cp2) -> cp1)) : Collections.emptyMap();

        List<CalendarTaskWithTagsAndPomodoro> resultList = new ArrayList<>();
        for (TaskWithTags taskWithTagsItem : tasksWithTags) {
            if (taskWithTagsItem == null || taskWithTagsItem.getTask() == null) {
                logger.warn(TAG, "processCombinedData: Encountered null TaskWithTags or null Task. Skipping.");
                continue;
            }
            CalendarParams params = paramsMap.get(taskWithTagsItem.getTask().getId());
            if (params == null) {
                logger.error(TAG, "CRITICAL: CalendarParams missing for task " + taskWithTagsItem.getTask().getId() + " during combine. SKIPPING THIS TASK.");
                // ВАЖНО: Если задача существует, а параметров календаря для нее нет, это ошибка целостности данных.
                // Это может быть причиной, почему задачи "не отображаются" - они отфильтровываются здесь.
                continue;
            }
            TaskStatistics stats = statsMap.get(taskWithTagsItem.getTask().getId());
            int pomodoroCount = (stats != null) ? stats.getCompletedPomodoroFocusSessions() : 0;
            resultList.add(new CalendarTaskWithTagsAndPomodoro(taskWithTagsItem.getTask(), params, taskWithTagsItem.getTags(), pomodoroCount));
            logger.debug(TAG, "processCombinedData: Added task to result: ID=" + taskWithTagsItem.getTask().getId() + ", Title='" + taskWithTagsItem.getTask().getTitle() + "'");
        }
        logger.info(TAG, "processCombinedData: Final combined list size: " + resultList.size());
        return resultList;
    }
}