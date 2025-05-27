package com.example.projectquestonjava.feature.statistics.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSessionRepository;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
import com.example.projectquestonjava.feature.statistics.data.model.GlobalStatistics;
import com.example.projectquestonjava.feature.statistics.data.model.TaskStatistics;
import com.example.projectquestonjava.feature.statistics.domain.repository.GamificationHistoryRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.GlobalStatisticsRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.TaskStatisticsRepository;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors; // Для directExecutor
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

@HiltViewModel
public class StatisticsViewModel extends ViewModel {

    private static final String TAG = "StatisticsViewModel";

    private final GlobalStatisticsRepository globalStatsRepository;
    private final TaskStatisticsRepository taskStatsRepository;
    private final PomodoroSessionRepository pomodoroSessionRepository;
    private final GamificationHistoryRepository gamificationHistoryRepository;
    private final UserSessionManager userSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final ScheduledExecutorService debounceExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledLoadTask;
    private final Logger logger;

    private final MutableLiveData<StatisticsScreenUiState> _uiStateLiveData =
            new MutableLiveData<>(StatisticsScreenUiState.createDefault());
    public final LiveData<StatisticsScreenUiState> uiStateLiveData = _uiStateLiveData;

    private final LiveData<Integer> userIdTrigger;
    private final MutableLiveData<StatsPeriod> _selectedPeriodTrigger = new MutableLiveData<>(StatsPeriod.WEEK);
    // ИСПРАВЛЕНИЕ: _customDateRangeTrigger инициализируется здесь
    private final MutableLiveData<DateTimeUtils.Pair<LocalDate, LocalDate>> _customDateRangeTrigger =
            new MutableLiveData<>(calculateDateRangeInternal(StatsPeriod.WEEK, LocalDate.now()));


    @Inject
    public StatisticsViewModel(
            GlobalStatisticsRepository globalStatsRepository,
            TaskStatisticsRepository taskStatsRepository,
            PomodoroSessionRepository pomodoroSessionRepository,
            GamificationHistoryRepository gamificationHistoryRepository,
            UserSessionManager userSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.globalStatsRepository = globalStatsRepository;
        this.taskStatsRepository = taskStatsRepository;
        this.pomodoroSessionRepository = pomodoroSessionRepository;
        this.gamificationHistoryRepository = gamificationHistoryRepository;
        this.userSessionManager = userSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;

        logger.debug(TAG, "Initializing StatisticsViewModel.");

        userIdTrigger = userSessionManager.getUserIdLiveData();

        MediatorLiveData<Object> loadTriggerMediator = new MediatorLiveData<>();
        loadTriggerMediator.addSource(userIdTrigger, id -> triggerLoadWithDebounce());
        loadTriggerMediator.addSource(_selectedPeriodTrigger, period -> {
            if (period != StatsPeriod.CUSTOM) {
                // При выборе стандартного периода, обновляем _customDateRangeTrigger,
                // что, в свою очередь, вызовет triggerLoadWithDebounce.
                _customDateRangeTrigger.setValue(calculateDateRangeInternal(period, LocalDate.now()));
            } else {
                // Если выбран CUSTOM, загрузка сработает от изменения _customDateRangeTrigger,
                // когда пользователь выберет даты в DatePicker.
                // Но если _customDateRangeTrigger уже содержит нужные даты (например, при восстановлении состояния),
                // можно вызвать загрузку и здесь. Для простоты, пока оставим так.
                // Если нужно принудительно обновить для CUSTOM при его выборе (без изменения дат),
                // можно добавить вызов triggerLoadWithDebounce() сюда.
                triggerLoadWithDebounce(); // Добавил на случай, если кастомный диапазон не менялся, а период стал CUSTOM
            }
        });
        // ИСПРАВЛЕНИЕ: Подписываемся на _customDateRangeTrigger
        loadTriggerMediator.addSource(_customDateRangeTrigger, range -> triggerLoadWithDebounce());


        loadTriggerMediator.observeForever(ignored -> {});


        Integer currentUserId = userIdTrigger.getValue();
        if (currentUserId != null && currentUserId != UserSessionManager.NO_USER_ID) {
            triggerLoadWithDebounce();
        } else if (currentUserId == null) {
            updateUiState(s -> s.toBuilder().isLoading(true).build());
        }
    }

    private void triggerLoadWithDebounce() {
        if (scheduledLoadTask != null && !scheduledLoadTask.isDone()) {
            scheduledLoadTask.cancel(false);
        }
        scheduledLoadTask = debounceExecutor.schedule(() -> {
            Integer userId = userIdTrigger.getValue();
            StatsPeriod currentSelectedPeriod = _selectedPeriodTrigger.getValue();
            // ИСПРАВЛЕНИЕ: Используем _customDateRangeTrigger.getValue()
            DateTimeUtils.Pair<LocalDate, LocalDate> dateRange = _customDateRangeTrigger.getValue();

            if (userId != null && userId != UserSessionManager.NO_USER_ID && dateRange != null && currentSelectedPeriod != null) {
                final LocalDate startDate = dateRange.first();
                final LocalDate endDate = dateRange.second();

                _uiStateLiveData.postValue(
                        Objects.requireNonNull(_uiStateLiveData.getValue()).toBuilder()
                                .selectedPeriod(currentSelectedPeriod)
                                .selectedStartDate(startDate)
                                .selectedEndDate(endDate)
                                .isLoading(true)
                                .error(null)
                                .build()
                );
                loadStatisticsForDateRangeInternal(startDate, endDate);
            } else if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "No user logged in, clearing statistics.");
                _uiStateLiveData.postValue(StatisticsScreenUiState.createDefault().withLoading(false));
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    // ... (остальная часть loadStatisticsForDateRangeInternal и другие методы остаются такими же, как в предыдущем ответе)
    private void loadStatisticsForDateRangeInternal(LocalDate startDate, LocalDate endDate) {
        logger.debug(TAG, "Loading statistics for range: " + startDate + " to " + endDate);

        ListenableFuture<GlobalStatistics> globalStatsFuture = globalStatsRepository.getGlobalStatisticsSuspend();
        ListenableFuture<List<TaskStatistics>> taskStatsPeriodFuture = taskStatsRepository.getTaskStatsInPeriod(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        ListenableFuture<List<PomodoroSession>> pomodoroSessionsPeriodFuture = pomodoroSessionRepository.getSessionsInPeriod(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        ListenableFuture<List<GamificationHistory>> gamificationHistoryPeriodFuture = gamificationHistoryRepository.getHistoryForPeriod(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        ListenableFuture<List<TaskStatistics>> allTaskStatsFuture = taskStatsRepository.getAllTaskStatisticsSuspend();

        List<ListenableFuture<?>> futures = List.of(
                globalStatsFuture, taskStatsPeriodFuture, pomodoroSessionsPeriodFuture,
                gamificationHistoryPeriodFuture, allTaskStatsFuture
        );

        ListenableFuture<List<Object>> allDataFuture = Futures.allAsList(futures);

        Futures.addCallback(allDataFuture, new FutureCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSuccess(List<Object> results) {
                try {
                    GlobalStatistics globalStats = (GlobalStatistics) results.get(0);
                    List<TaskStatistics> taskStatsInPeriod = (List<TaskStatistics>) results.get(1);
                    List<PomodoroSession> pomodoroSessionsInPeriod = (List<PomodoroSession>) results.get(2);
                    List<GamificationHistory> gamificationHistoryInPeriod = (List<GamificationHistory>) results.get(3);
                    List<TaskStatistics> allTaskStats = (List<TaskStatistics>) results.get(4);

                    Float completionRateOverall = calculateCompletionRate(globalStats);
                    float avgTasksOverall = calculateAverageTasksPerDay(allTaskStats, globalStats);
                    DayOfWeek mostProductiveDayOverall = findMostProductiveDayOfWeek(allTaskStats);

                    Map<LocalDate, Integer> completedTasksByDayMap = aggregateTasksByDay(taskStatsInPeriod, startDate, endDate);
                    Map<LocalDate, Integer> pomodoroMinutesByDayMap = aggregatePomodoroByDay(pomodoroSessionsInPeriod, startDate, endDate);
                    Map<LocalDate, DateTimeUtils.Pair<Integer, Integer>> gamificationChangesByDayMap = aggregateGamificationByDay(gamificationHistoryInPeriod, startDate, endDate);
                    Map<DayOfWeek, Integer> tasksByDayOfWeekMap = aggregateTasksByDayOfWeek(taskStatsInPeriod);

                    List<DatePoint> taskCompletionTrend = mapToDatePoints(completedTasksByDayMap, startDate, endDate);
                    List<DatePoint> pomodoroFocusTrend = mapToDatePoints(pomodoroMinutesByDayMap, startDate, endDate);
                    List<DatePoint> xpGainTrend = mapToGamificationDatePoints(gamificationChangesByDayMap, startDate, endDate, DateTimeUtils.Pair::first);
                    List<DatePoint> coinGainTrend = mapToGamificationDatePoints(gamificationChangesByDayMap, startDate, endDate, DateTimeUtils.Pair::second);
                    List<DayOfWeekPoint> tasksCompletedByDayOfWeek = mapToDayOfWeekPoints(tasksByDayOfWeekMap);

                    int totalTasksCompletedInPeriod = (int) taskStatsInPeriod.stream().filter(ts -> ts.getCompletionTime() != null).count();
                    int totalPomodoroSecondsInPeriod = pomodoroSessionsInPeriod.stream().mapToInt(PomodoroSession::getActualDurationSeconds).sum();
                    long numberOfDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                    float averageDailyPomodoroMinutes = (numberOfDays > 0) ? (float) totalPomodoroSecondsInPeriod / 60 / numberOfDays : 0f;
                    int totalXpGainedInPeriod = gamificationHistoryInPeriod.stream().mapToInt(GamificationHistory::getXpChange).sum();
                    int totalCoinsGainedInPeriod = gamificationHistoryInPeriod.stream().mapToInt(GamificationHistory::getCoinsChange).sum();
                    LocalDate mostProductiveDayInPeriod = completedTasksByDayMap.entrySet().stream()
                            .filter(entry -> entry.getValue() > 0)
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    logger.debug(TAG, "Data processed successfully for " + startDate + " - " + endDate + ".");
                    _uiStateLiveData.postValue(
                            Objects.requireNonNull(_uiStateLiveData.getValue()).toBuilder()
                                    .isLoading(false)
                                    .globalStats(globalStats)
                                    .taskCompletionRateOverall(completionRateOverall)
                                    .averageTasksPerDayOverall(avgTasksOverall)
                                    .mostProductiveDayOfWeekOverall(mostProductiveDayOverall)
                                    .taskCompletionTrend(taskCompletionTrend)
                                    .pomodoroFocusTrend(pomodoroFocusTrend)
                                    .xpGainTrend(xpGainTrend)
                                    .coinGainTrend(coinGainTrend)
                                    .tasksCompletedByDayOfWeek(tasksCompletedByDayOfWeek)
                                    .totalTasksCompletedInPeriod(totalTasksCompletedInPeriod)
                                    .totalPomodoroMinutesInPeriod(totalPomodoroSecondsInPeriod / 60)
                                    .averageDailyPomodoroMinutes(averageDailyPomodoroMinutes)
                                    .totalXpGainedInPeriod(totalXpGainedInPeriod)
                                    .totalCoinsGainedInPeriod(totalCoinsGainedInPeriod)
                                    .mostProductiveDayInPeriod(mostProductiveDayInPeriod)
                                    .error(null)
                                    .build()
                    );
                } catch (Exception e) {
                    logger.error(TAG, "Error processing fetched stats data in callback", e);
                    _uiStateLiveData.postValue(Objects.requireNonNull(_uiStateLiveData.getValue()).withLoading(false).withError("Ошибка обработки данных: " + e.getMessage()));
                }
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Error loading statistics for " + startDate + " - " + endDate, t);
                _uiStateLiveData.postValue(Objects.requireNonNull(_uiStateLiveData.getValue()).withLoading(false).withError("Ошибка загрузки статистики: " + t.getMessage()));
            }
        }, ioExecutor);
    }

    private Map<LocalDate, Integer> aggregateTasksByDay(List<TaskStatistics> stats, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Integer> dateMap = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) { dateMap.put(date, 0); }
        if (stats != null) {
            stats.stream().filter(s -> s.getCompletionTime() != null).forEach(s -> {
                LocalDate date = dateTimeUtils.utcToLocalLocalDateTime(s.getCompletionTime()).toLocalDate();
                dateMap.computeIfPresent(date, (k, v) -> v + 1);
            });
        } return dateMap;
    }
    private Map<LocalDate, Integer> aggregatePomodoroByDay(List<PomodoroSession> sessions, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Integer> dateMap = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) { dateMap.put(date, 0); }
        if (sessions != null) {
            sessions.forEach(s -> {
                if (s.getSessionType() == SessionType.FOCUS) {
                    LocalDate date = dateTimeUtils.utcToLocalLocalDateTime(s.getStartTime()).toLocalDate();
                    dateMap.computeIfPresent(date, (k, v) -> v + (s.getActualDurationSeconds() / 60));
                }
            });
        } return dateMap;
    }
    private Map<LocalDate, DateTimeUtils.Pair<Integer, Integer>> aggregateGamificationByDay(List<GamificationHistory> history, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, DateTimeUtils.Pair<Integer, Integer>> dateMap = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) { dateMap.put(date, new DateTimeUtils.Pair<>(0,0));}
        if (history != null) {
            history.forEach(h -> {
                LocalDate date = dateTimeUtils.utcToLocalLocalDateTime(h.getTimestamp()).toLocalDate();
                dateMap.computeIfPresent(date, (k, current) -> new DateTimeUtils.Pair<>(current.first() + h.getXpChange(), current.second() + h.getCoinsChange()));
            });
        } return dateMap;
    }
    private Map<DayOfWeek, Integer> aggregateTasksByDayOfWeek(List<TaskStatistics> stats) {
        if (stats == null) return Collections.emptyMap();
        return stats.stream().filter(s -> s.getCompletionTime() != null)
                .collect(Collectors.groupingBy(s -> dateTimeUtils.utcToLocalLocalDateTime(s.getCompletionTime()).getDayOfWeek(), Collectors.summingInt(s -> 1)));
    }
    private Float calculateCompletionRate(GlobalStatistics gs) {
        return (gs == null || gs.getTotalTasks() <= 0) ? null : Math.max(0f, Math.min(1f, (float) gs.getCompletedTasks() / gs.getTotalTasks()));
    }
    private float calculateAverageTasksPerDay(List<TaskStatistics> allStats, GlobalStatistics gs) {
        if (allStats == null || allStats.isEmpty()) return 0f;
        LocalDateTime firstActivityDate = null;
        if (gs != null && gs.getLastActive() != null) {
            firstActivityDate = gs.getLastActive();
        }
        LocalDateTime firstCompletion = allStats.stream()
                .map(TaskStatistics::getCompletionTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        if (firstCompletion != null && (firstActivityDate == null || firstCompletion.isBefore(firstActivityDate))) {
            firstActivityDate = firstCompletion;
        }
        if (firstActivityDate == null) return 0f;

        LocalDate startDate = dateTimeUtils.utcToLocalLocalDateTime(firstActivityDate).toLocalDate();
        long daysActive = ChronoUnit.DAYS.between(startDate, dateTimeUtils.currentLocalDate()) + 1;
        daysActive = Math.max(1, daysActive);
        long totalCompleted = allStats.stream().filter(s->s.getCompletionTime() != null).count();
        return (float) totalCompleted / daysActive;
    }
    private DayOfWeek findMostProductiveDayOfWeek(List<TaskStatistics> allStats) {
        if (allStats == null) return null;
        return aggregateTasksByDayOfWeek(allStats).entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }
    private List<DatePoint> mapToDatePoints(Map<LocalDate, Integer> data, LocalDate startDate, LocalDate endDate) {
        List<DatePoint> points = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            points.add(new DatePoint(date, Objects.requireNonNull(data.getOrDefault(date, 0)).floatValue()));
        } return points;
    }
    private List<DatePoint> mapToGamificationDatePoints(Map<LocalDate, DateTimeUtils.Pair<Integer,Integer>> data, LocalDate startDate, LocalDate endDate, Function<DateTimeUtils.Pair<Integer,Integer>, Integer> valueSelector) {
        List<DatePoint> points = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DateTimeUtils.Pair<Integer,Integer> pair = data.getOrDefault(date, new DateTimeUtils.Pair<>(0,0));
            points.add(new DatePoint(date, valueSelector.apply(pair).floatValue()));
        } return points;
    }
    private List<DayOfWeekPoint> mapToDayOfWeekPoints(Map<DayOfWeek, Integer> data) {
        List<DayOfWeekPoint> points = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            points.add(new DayOfWeekPoint(day, Objects.requireNonNull(data.getOrDefault(day, 0)).floatValue()));
        }
        points.sort(Comparator.comparingInt(p -> p.dayOfWeek().getValue()));
        return points;
    }


    public void selectPeriod(StatsPeriod period) {
        _selectedPeriodTrigger.setValue(period);
    }

    public void selectCustomDateRange(LocalDate startDate, LocalDate endDate) {
        _customDateRangeTrigger.setValue(new DateTimeUtils.Pair<>(startDate, endDate));
        _selectedPeriodTrigger.setValue(StatsPeriod.CUSTOM); // Устанавливаем тип периода
    }

    public void clearError() {
        updateUiState(s -> s.withError(null));
    }

    private DateTimeUtils.Pair<LocalDate, LocalDate> calculateDateRangeInternal(StatsPeriod period, LocalDate today) {
        if (period == null) period = StatsPeriod.WEEK;
        return switch (period) {
            case WEEK -> new DateTimeUtils.Pair<>(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today);
            case MONTH -> new DateTimeUtils.Pair<>(today.with(TemporalAdjusters.firstDayOfMonth()), today);
            case ALL_TIME -> {
                GlobalStatistics gs = null;
                StatisticsScreenUiState currentState = _uiStateLiveData.getValue();
                if (currentState != null) gs = currentState.getGlobalStats();
                LocalDate veryFirstDay;
                if ((gs != null && gs.getLastActive() != null)) {
                    assert dateTimeUtils != null;
                    veryFirstDay = dateTimeUtils.utcToLocalLocalDateTime(gs.getLastActive()).toLocalDate();
                } else {
                    veryFirstDay = today.minusYears(5);
                }
                yield new DateTimeUtils.Pair<>(veryFirstDay.with(TemporalAdjusters.firstDayOfYear()), today);
            }
            case CUSTOM -> {
                DateTimeUtils.Pair<LocalDate, LocalDate> currentCustom = _customDateRangeTrigger.getValue();
                yield Objects.requireNonNullElseGet(currentCustom, () -> new DateTimeUtils.Pair<>(today.minusDays(6), today));
            }
        };
    }

    private void updateUiState(Function<StatisticsScreenUiState, StatisticsScreenUiState> updater) {
        StatisticsScreenUiState current = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.apply(current != null ? current : StatisticsScreenUiState.createDefault()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!debounceExecutor.isShutdown()) {
            debounceExecutor.shutdownNow();
        }
        logger.debug(TAG, "StatisticsViewModel cleared.");
    }
}