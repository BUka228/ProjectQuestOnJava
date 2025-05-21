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
import com.google.common.util.concurrent.MoreExecutors;

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
        logger.debug(TAG, "Getting tasks for day: workspace=" + workspaceId + ", start=" + boundaries.first() + ", end=" + boundaries.second());

        return Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "No user for getTasksForDay.");
                MutableLiveData<List<CalendarTaskWithTagsAndPomodoro>> emptyLiveData = new MutableLiveData<>();
                emptyLiveData.setValue(Collections.emptyList());
                return emptyLiveData;
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
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "No user for getTasksForMonth.");
                MutableLiveData<List<CalendarTaskWithTagsAndPomodoro>> emptyLiveData = new MutableLiveData<>();
                emptyLiveData.setValue(Collections.emptyList());
                return emptyLiveData;
            }
            LiveData<List<TaskWithTags>> tasksWithTagsLiveData = taskDao.getTasksWithTagsForWorkspaceInDateRange(workspaceId, userId, boundaries.first(), boundaries.second());
            return combineWithStatsAndParamsLiveData(tasksWithTagsLiveData);
        });
    }

    @Override
    public ListenableFuture<CalendarTaskWithTagsAndPomodoro> getTaskWithTagsAndPomodoroById(long workspaceId, long taskId) {
        // Получаем userId асинхронно
        return Futures.submitAsync(() -> { // Обертка для получения userId
            int userId = userSessionManager.getUserIdSync(); // Блокирующий вызов, но мы на ioExecutor
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
                // Проверка Workspace ID
                if (taskWithTags.getTask().getWorkspaceId() != workspaceId && workspaceId != -1L) {
                    logger.warn(TAG, "Task " + taskId + " belongs to workspace " + taskWithTags.getTask().getWorkspaceId() + ", but requested for " + workspaceId + ".");
                    return Futures.immediateFuture(null);
                }

                ListenableFuture<TaskStatistics> statsFuture = taskStatisticsRepository.getStatisticsForTask(taskId);
                ListenableFuture<CalendarParams> paramsFuture = calendarParamsRepository.getParamsByTaskId(taskId);

                // Объединяем ListenableFuture
                return Futures.whenAllSucceed(statsFuture, paramsFuture)
                        .call(() -> {
                            TaskStatistics stats = Futures.getDone(statsFuture); // Безопасно, т.к. whenAllSucceed
                            CalendarParams params = Futures.getDone(paramsFuture);

                            if (params == null) {
                                logger.error(TAG, "CRITICAL: CalendarParams missing for existing task " + taskWithTags.getTask().getId() + ".");
                                return null; // Или бросить исключение
                            }

                            int pomodoroCount = (stats != null) ? stats.getCompletedPomodoroFocusSessions() : 0;
                            return new CalendarTaskWithTagsAndPomodoro(taskWithTags.getTask(), params, taskWithTags.getTags(), pomodoroCount);
                        }, ioExecutor); // Выполняем финальное преобразование на ioExecutor
            }, ioExecutor); // Выполняем transformAsync для taskWithTagsFuture на ioExecutor
        }, ioExecutor); // Выполняем submitAsync на ioExecutor
    }


    private LiveData<List<CalendarTaskWithTagsAndPomodoro>> combineWithStatsAndParamsLiveData(LiveData<List<TaskWithTags>> tasksWithTagsLiveData) {
        MediatorLiveData<List<CalendarTaskWithTagsAndPomodoro>> resultLiveData = new MediatorLiveData<>();
        resultLiveData.setValue(Collections.emptyList()); // Начальное значение

        // Источник для отслеживания задач
        resultLiveData.addSource(tasksWithTagsLiveData, tasksWithTags -> {
            if (tasksWithTags == null || tasksWithTags.isEmpty()) {
                resultLiveData.setValue(Collections.emptyList());
                return;
            }
            List<Long> taskIds = tasksWithTags.stream().map(twt -> twt.getTask().getId()).collect(Collectors.toList());
            logger.debug(TAG, "Combining details for tasks: " + taskIds);

            // Получаем LiveData для статистики и параметров
            LiveData<List<TaskStatistics>> statsLiveData = taskStatisticsRepository.getStatisticsForTasksFlow(taskIds);
            LiveData<List<CalendarParams>> paramsLiveData = calendarParamsRepository.getParamsForTasksFlow(taskIds);

            // Объединяем три LiveData
            CombinedLiveDataSources combinedSources = new CombinedLiveDataSources(tasksWithTags, statsLiveData, paramsLiveData);

            // Удаляем предыдущие источники от statsLiveData и paramsLiveData, если они были
            // Это важно, чтобы не было утечек и лишних срабатываний от старых подписок
            // (Этот код должен быть более надежным в реальном приложении, здесь упрощенно)
            resultLiveData.removeSource(statsLiveData); // Попытка удалить, если был добавлен ранее
            resultLiveData.removeSource(paramsLiveData);

            resultLiveData.addSource(combinedSources, data -> {
                if (data != null) {
                    List<CalendarTaskWithTagsAndPomodoro> processedList = processCombinedData(data.tasksWithTags, data.stats, data.params);
                    resultLiveData.setValue(processedList);
                } else {
                    resultLiveData.setValue(Collections.emptyList());
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

        CombinedLiveDataSources(List<TaskWithTags> initialTasks, LiveData<List<TaskStatistics>> statsSource, LiveData<List<CalendarParams>> paramsSource) {
            this.tasksWithTags = initialTasks; // Задачи уже есть

            addSource(statsSource, s -> {
                stats = s;
                tryUpdateValue();
            });
            addSource(paramsSource, p -> {
                params = p;
                tryUpdateValue();
            });
            // Попытка обновить значение сразу, если все данные уже есть (маловероятно для LiveData из БД)
            tryUpdateValue();
        }

        private void tryUpdateValue() {
            if (tasksWithTags != null && stats != null && params != null) {
                setValue(new CombinedData(tasksWithTags, stats, params));
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

        if (tasksWithTags == null || tasksWithTags.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, TaskStatistics> statsMap = (statsList != null) ?
                statsList.stream().collect(Collectors.toMap(TaskStatistics::getTaskId, ts -> ts, (ts1, ts2) -> ts1)) : Collections.emptyMap();
        Map<Long, CalendarParams> paramsMap = (paramsList != null) ?
                paramsList.stream().collect(Collectors.toMap(CalendarParams::getTaskId, cp -> cp, (cp1, cp2) -> cp1)) : Collections.emptyMap();

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
        logger.debug(TAG, "Combine result size: " + resultList.size());
        return resultList;
    }
}