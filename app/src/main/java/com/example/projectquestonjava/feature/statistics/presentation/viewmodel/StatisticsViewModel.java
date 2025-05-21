package com.example.projectquestonjava.feature.statistics.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
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
import com.google.common.util.concurrent.MoreExecutors;

import dagger.hilt.android.lifecycle.HiltViewModel;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
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
import java.util.stream.Collectors;
import javax.inject.Inject;

// Вспомогательные классы (если еще не определены глобально)
enum StatsPeriod { WEEK, MONTH, ALL_TIME, CUSTOM }

class DatePoint {
    public final LocalDate date;
    public final float value;
    public DatePoint(LocalDate date, float value) { this.date = date; this.value = value; }
}

class DayOfWeekPoint {
    public final DayOfWeek dayOfWeek;
    public final float value;
    public DayOfWeekPoint(DayOfWeek dayOfWeek, float value) { this.dayOfWeek = dayOfWeek; this.value = value; }
}

class StatisticsScreenUiState {
    public final StatsPeriod selectedPeriod;
    public final boolean isLoading;
    public final String error;
    public final LocalDate selectedStartDate;
    public final LocalDate selectedEndDate;
    public final GlobalStatistics globalStats;
    public final Float taskCompletionRateOverall;
    public final float averageTasksPerDayOverall;
    public final DayOfWeek mostProductiveDayOfWeekOverall;
    public final List<DatePoint> taskCompletionTrend;
    public final List<DatePoint> pomodoroFocusTrend;
    public final List<DatePoint> xpGainTrend;
    public final List<DatePoint> coinGainTrend;
    public final List<DayOfWeekPoint> tasksCompletedByDayOfWeek;
    public final int totalTasksCompletedInPeriod;
    public final int totalPomodoroMinutesInPeriod;
    public final float averageDailyPomodoroMinutes;
    public final int totalXpGainedInPeriod;
    public final int totalCoinsGainedInPeriod;
    public final LocalDate mostProductiveDayInPeriod;

    public StatisticsScreenUiState(StatsPeriod selectedPeriod, boolean isLoading, String error,
                                   LocalDate selectedStartDate, LocalDate selectedEndDate,
                                   GlobalStatistics globalStats, Float taskCompletionRateOverall,
                                   float averageTasksPerDayOverall, DayOfWeek mostProductiveDayOfWeekOverall,
                                   List<DatePoint> taskCompletionTrend, List<DatePoint> pomodoroFocusTrend,
                                   List<DatePoint> xpGainTrend, List<DatePoint> coinGainTrend,
                                   List<DayOfWeekPoint> tasksCompletedByDayOfWeek,
                                   int totalTasksCompletedInPeriod, int totalPomodoroMinutesInPeriod,
                                   float averageDailyPomodoroMinutes, int totalXpGainedInPeriod,
                                   int totalCoinsGainedInPeriod, LocalDate mostProductiveDayInPeriod) {
        this.selectedPeriod = selectedPeriod;
        this.isLoading = isLoading;
        this.error = error;
        this.selectedStartDate = selectedStartDate;
        this.selectedEndDate = selectedEndDate;
        this.globalStats = globalStats;
        this.taskCompletionRateOverall = taskCompletionRateOverall;
        this.averageTasksPerDayOverall = averageTasksPerDayOverall;
        this.mostProductiveDayOfWeekOverall = mostProductiveDayOfWeekOverall;
        this.taskCompletionTrend = taskCompletionTrend != null ? taskCompletionTrend : Collections.emptyList();
        this.pomodoroFocusTrend = pomodoroFocusTrend != null ? pomodoroFocusTrend : Collections.emptyList();
        this.xpGainTrend = xpGainTrend != null ? xpGainTrend : Collections.emptyList();
        this.coinGainTrend = coinGainTrend != null ? coinGainTrend : Collections.emptyList();
        this.tasksCompletedByDayOfWeek = tasksCompletedByDayOfWeek != null ? tasksCompletedByDayOfWeek : Collections.emptyList();
        this.totalTasksCompletedInPeriod = totalTasksCompletedInPeriod;
        this.totalPomodoroMinutesInPeriod = totalPomodoroMinutesInPeriod;
        this.averageDailyPomodoroMinutes = averageDailyPomodoroMinutes;
        this.totalXpGainedInPeriod = totalXpGainedInPeriod;
        this.totalCoinsGainedInPeriod = totalCoinsGainedInPeriod;
        this.mostProductiveDayInPeriod = mostProductiveDayInPeriod;
    }

    // Конструктор по умолчанию
    public StatisticsScreenUiState() {
        this(StatsPeriod.WEEK, true, null,
                LocalDate.now().with(DayOfWeek.MONDAY), LocalDate.now(),
                null, null, 0f, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                DayOfWeek.values().length > 0 ?
                        java.util.Arrays.stream(DayOfWeek.values()).map(day -> new DayOfWeekPoint(day, 0f)).collect(Collectors.toList()) :
                        Collections.emptyList(),
                0, 0, 0f, 0, 0, null);
    }

    public StatisticsScreenUiState copy(
            StatsPeriod selectedPeriod, Boolean isLoading, String error,
            LocalDate selectedStartDate, LocalDate selectedEndDate,
            GlobalStatistics globalStats, Float taskCompletionRateOverall,
            Float averageTasksPerDayOverall, DayOfWeek mostProductiveDayOfWeekOverall,
            List<DatePoint> taskCompletionTrend, List<DatePoint> pomodoroFocusTrend,
            List<DatePoint> xpGainTrend, List<DatePoint> coinGainTrend,
            List<DayOfWeekPoint> tasksCompletedByDayOfWeek,
            Integer totalTasksCompletedInPeriod, Integer totalPomodoroMinutesInPeriod,
            Float averageDailyPomodoroMinutes, Integer totalXpGainedInPeriod,
            Integer totalCoinsGainedInPeriod, LocalDate mostProductiveDayInPeriod,
            boolean clearError // Флаг для явной очистки ошибки
    ) {
        return new StatisticsScreenUiState(
                selectedPeriod != null ? selectedPeriod : this.selectedPeriod,
                isLoading != null ? isLoading : this.isLoading,
                clearError ? null : (error != null ? error : this.error),
                selectedStartDate != null ? selectedStartDate : this.selectedStartDate,
                selectedEndDate != null ? selectedEndDate : this.selectedEndDate,
                globalStats != null ? globalStats : this.globalStats,
                taskCompletionRateOverall, // Может быть null
                averageTasksPerDayOverall != null ? averageTasksPerDayOverall : this.averageTasksPerDayOverall,
                mostProductiveDayOfWeekOverall, // Может быть null
                taskCompletionTrend != null ? taskCompletionTrend : this.taskCompletionTrend,
                pomodoroFocusTrend != null ? pomodoroFocusTrend : this.pomodoroFocusTrend,
                xpGainTrend != null ? xpGainTrend : this.xpGainTrend,
                coinGainTrend != null ? coinGainTrend : this.coinGainTrend,
                tasksCompletedByDayOfWeek != null ? tasksCompletedByDayOfWeek : this.tasksCompletedByDayOfWeek,
                totalTasksCompletedInPeriod != null ? totalTasksCompletedInPeriod : this.totalTasksCompletedInPeriod,
                totalPomodoroMinutesInPeriod != null ? totalPomodoroMinutesInPeriod : this.totalPomodoroMinutesInPeriod,
                averageDailyPomodoroMinutes != null ? averageDailyPomodoroMinutes : this.averageDailyPomodoroMinutes,
                totalXpGainedInPeriod != null ? totalXpGainedInPeriod : this.totalXpGainedInPeriod,
                totalCoinsGainedInPeriod != null ? totalCoinsGainedInPeriod : this.totalCoinsGainedInPeriod,
                mostProductiveDayInPeriod // Может быть null
        );
    }
}


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
    private final Logger logger;

    private final MutableLiveData<StatisticsScreenUiState> _uiStateLiveData = new MutableLiveData<>(new StatisticsScreenUiState());
    public LiveData<StatisticsScreenUiState> uiStateLiveData = _uiStateLiveData;

    // Для эмуляции debounce и flatMapLatest
    private final ScheduledExecutorService debounceExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledLoadTask;

    // LiveData для триггеров
    private final MutableLiveData<StatsPeriod> _selectedPeriodLiveData = new MutableLiveData<>(StatsPeriod.WEEK);
    private final MutableLiveData<Pair<LocalDate, LocalDate>> _customDateRangeLiveData =
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

        logger.debug(TAG, "Initializing ViewModel.");

        // Объединяем триггеры для загрузки данных
        MediatorLiveData<Object> loadTrigger = new MediatorLiveData<>();
        loadTrigger.addSource(userSessionManager.getUserIdLiveData(), id -> triggerLoadWithDebounce());
        loadTrigger.addSource(_selectedPeriodLiveData, period -> triggerLoadWithDebounce());
        loadTrigger.addSource(_customDateRangeLiveData, range -> triggerLoadWithDebounce());

        // Начальная загрузка (если userId уже есть)
        if (userSessionManager.getUserIdSync() != UserSessionManager.NO_USER_ID) {
            triggerLoadWithDebounce();
        }
        // Наблюдаем за loadTrigger, чтобы запустить загрузку, но сама загрузка происходит
        // через triggerLoadWithDebounce, который использует debounceExecutor.
        // Это немного обходной путь для эмуляции debounce + collectLatest.
        loadTrigger.observeForever(ignored -> {}); // Пустой наблюдатель, чтобы MediatorLiveData был активен
    }

    private void triggerLoadWithDebounce() {
        if (scheduledLoadTask != null && !scheduledLoadTask.isDone()) {
            scheduledLoadTask.cancel(false);
        }
        scheduledLoadTask = debounceExecutor.schedule(() -> {
            int userId = userSessionManager.getUserIdSync(); // Получаем актуальный ID
            StatsPeriod period = _selectedPeriodLiveData.getValue();
            Pair<LocalDate, LocalDate> customRange = _customDateRangeLiveData.getValue();

            if (userId != UserSessionManager.NO_USER_ID && period != null) {
                Pair<LocalDate, LocalDate> dateRange = (period == StatsPeriod.CUSTOM && customRange != null) ?
                        customRange : calculateDateRangeInternal(period, LocalDate.now());
                // Обновляем UI State датами ПЕРЕД загрузкой
                final LocalDate finalStartDate = dateRange.first;
                final LocalDate finalEndDate = dateRange.second;
                updateUiState(s -> s.copy(period, true, null, finalStartDate, finalEndDate, null,null,null,null,null,null,null,null,null,null,null,null,null,null,null, false));
                loadStatisticsForDateRangeInternal(finalStartDate, finalEndDate);
            } else if (userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "No user logged in, clearing statistics.");
                _uiStateLiveData.postValue(new StatisticsScreenUiState().copy(null, false, null, null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,false));
            }
        }, 300, TimeUnit.MILLISECONDS);
    }


    private void loadStatisticsForDateRangeInternal(LocalDate startDate, LocalDate endDate) {
        logger.debug(TAG, "Loading statistics for range: " + startDate + " to " + endDate);
        updateUiState(s -> s.copy(null,true, null, null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,true));


        ListenableFuture<GlobalStatistics> globalStatsFuture = globalStatsRepository.getGlobalStatisticsSuspend();
        ListenableFuture<List<TaskStatistics>> taskStatsPeriodFuture = taskStatsRepository.getTaskStatsInPeriod(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        ListenableFuture<List<PomodoroSession>> pomodoroSessionsPeriodFuture = pomodoroSessionRepository.getSessionsInPeriod(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        ListenableFuture<List<GamificationHistory>> gamificationHistoryPeriodFuture = gamificationHistoryRepository.getHistoryForPeriod(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        ListenableFuture<List<TaskStatistics>> allTaskStatsFuture = taskStatsRepository.getAllTaskStatisticsSuspend();

        ListenableFuture<List<Object>> allDataFuture = Futures.allAsList(
                globalStatsFuture, taskStatsPeriodFuture, pomodoroSessionsPeriodFuture,
                gamificationHistoryPeriodFuture, allTaskStatsFuture
        );

        Futures.addCallback(allDataFuture, new FutureCallback<List<Object>>() {
            @Override
            public void onSuccess(List<Object> results) {
                try {
                    GlobalStatistics globalStats = (GlobalStatistics) results.get(0);
                    List<TaskStatistics> taskStatsInPeriod = (List<TaskStatistics>) results.get(1);
                    List<PomodoroSession> pomodoroSessionsInPeriod = (List<PomodoroSession>) results.get(2);
                    List<GamificationHistory> gamificationHistoryInPeriod = (List<GamificationHistory>) results.get(3);
                    List<TaskStatistics> allTaskStats = (List<TaskStatistics>) results.get(4);

                    logger.debug(TAG, "Data fetched: Global=" + (globalStats != null) +
                            ", TasksPeriod=" + taskStatsInPeriod.size() +
                            ", Pomodoro=" + pomodoroSessionsInPeriod.size() +
                            ", History=" + gamificationHistoryInPeriod.size() +
                            ", AllTasks=" + allTaskStats.size());

                    // --- Вычисления --- (такие же, как в Kotlin)
                    Float completionRateOverall = calculateCompletionRate(globalStats);
                    float avgTasksOverall = calculateAverageTasksPerDay(allTaskStats, globalStats);
                    DayOfWeek mostProductiveDayOverall = findMostProductiveDayOfWeek(allTaskStats);

                    Map<LocalDate, Integer> completedTasksByDayMap = aggregateTasksByDay(taskStatsInPeriod, startDate, endDate);
                    Map<LocalDate, Integer> pomodoroMinutesByDayMap = aggregatePomodoroByDay(pomodoroSessionsInPeriod, startDate, endDate);
                    Map<LocalDate, Pair<Integer, Integer>> gamificationChangesByDayMap = aggregateGamificationByDay(gamificationHistoryInPeriod, startDate, endDate);
                    Map<DayOfWeek, Integer> tasksByDayOfWeekMap = aggregateTasksByDayOfWeek(taskStatsInPeriod);

                    List<DatePoint> taskCompletionTrend = mapToDatePoints(completedTasksByDayMap, startDate, endDate);
                    List<DatePoint> pomodoroFocusTrend = mapToDatePoints(pomodoroMinutesByDayMap, startDate, endDate);
                    List<DatePoint> xpGainTrend = mapToGamificationDatePoints(gamificationChangesByDayMap, startDate, endDate, Pair::getFirst);
                    List<DatePoint> coinGainTrend = mapToGamificationDatePoints(gamificationChangesByDayMap, startDate, endDate, Pair::getSecond);
                    List<DayOfWeekPoint> tasksCompletedByDayOfWeek = mapToDayOfWeekPoints(tasksByDayOfWeekMap);

                    int totalTasksCompletedInPeriod = taskStatsInPeriod.stream().filter(ts -> ts.getCompletionTime() != null).mapToInt(ts -> 1).sum();
                    int totalPomodoroMinutesInPeriod = pomodoroSessionsInPeriod.stream().mapToInt(PomodoroSession::getActualDurationSeconds).sum() / 60;
                    long numberOfDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                    float averageDailyPomodoroMinutes = (numberOfDays > 0) ? (float) totalPomodoroMinutesInPeriod / numberOfDays : 0f;
                    int totalXpGainedInPeriod = gamificationHistoryInPeriod.stream().mapToInt(GamificationHistory::getXpChange).sum();
                    int totalCoinsGainedInPeriod = gamificationHistoryInPeriod.stream().mapToInt(GamificationHistory::getCoinsChange).sum();
                    LocalDate mostProductiveDayInPeriod = completedTasksByDayMap.entrySet().stream()
                            .filter(entry -> entry.getValue() > 0)
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);

                    updateUiState(s -> s.copy(
                            null, false, null, null, null,
                            globalStats, completionRateOverall, avgTasksOverall, mostProductiveDayOverall,
                            taskCompletionTrend, pomodoroFocusTrend, xpGainTrend, coinGainTrend,
                            tasksCompletedByDayOfWeek, totalTasksCompletedInPeriod, totalPomodoroMinutesInPeriod,
                            averageDailyPomodoroMinutes, totalXpGainedInPeriod, totalCoinsGainedInPeriod,
                            mostProductiveDayInPeriod, false
                    ));

                } catch (Exception e) { // Ловим исключения при обработке результатов
                    logger.error(TAG, "Error processing fetched statistics data", e);
                    updateUiState(s -> s.copy(null, false, "Ошибка обработки данных: " + e.getMessage(), null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null, false));
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Error loading statistics for " + startDate + " - " + endDate, t);
                updateUiState(s -> s.copy(null, false, "Ошибка загрузки статистики: " + t.getMessage(), null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null, false));
            }
        }, ioExecutor); // Выполняем коллбэк на ioExecutor
    }


    // --- Функции агрегации (такие же, как в Kotlin ViewModel, адаптированные под Java) ---
    private Map<LocalDate, Integer> aggregateTasksByDay(List<TaskStatistics> stats, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Integer> dateMap = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateMap.put(date, 0);
        }
        if (stats != null) {
            stats.stream()
                    .filter(s -> s.getCompletionTime() != null)
                    .forEach(s -> {
                        LocalDate date = dateTimeUtils.utcToLocalLocalDateTime(s.getCompletionTime()).toLocalDate();
                        if (dateMap.containsKey(date)) {
                            dateMap.put(date, dateMap.get(date) + 1);
                        }
                    });
        }
        return dateMap;
    }

    private Map<LocalDate, Integer> aggregatePomodoroByDay(List<PomodoroSession> sessions, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Integer> dateMap = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateMap.put(date, 0);
        }
        if (sessions != null) {
            sessions.forEach(s -> {
                LocalDate date = dateTimeUtils.utcToLocalLocalDateTime(s.getStartTime()).toLocalDate();
                if (dateMap.containsKey(date)) {
                    dateMap.put(date, dateMap.get(date) + (s.getActualDurationSeconds() / 60)); // Суммируем минуты
                }
            });
        }
        return dateMap;
    }

    private Map<LocalDate, Pair<Integer, Integer>> aggregateGamificationByDay(List<GamificationHistory> history, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Pair<Integer, Integer>> dateMap = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateMap.put(date, new Pair<>(0, 0));
        }
        if (history != null) {
            history.forEach(h -> {
                LocalDate date = dateTimeUtils.utcToLocalLocalDateTime(h.getTimestamp()).toLocalDate();
                if (dateMap.containsKey(date)) {
                    dateMap.computeIfPresent(date, (k, current) -> new Pair<>(current.first + h.getXpChange(), current.second + h.getCoinsChange()));
                }
            });
        }
        return dateMap;
    }

    private Map<DayOfWeek, Integer> aggregateTasksByDayOfWeek(List<TaskStatistics> stats) {
        if (stats == null) return Collections.emptyMap();
        return stats.stream()
                .filter(s -> s.getCompletionTime() != null)
                .collect(Collectors.groupingBy(
                        s -> dateTimeUtils.utcToLocalLocalDateTime(s.getCompletionTime()).getDayOfWeek(),
                        Collectors.summingInt(s -> 1)
                ));
    }

    private Float calculateCompletionRate(GlobalStatistics globalStats) {
        if (globalStats == null || globalStats.getTotalTasks() <= 0) return null;
        return Math.max(0f, Math.min(1f, (float) globalStats.getCompletedTasks() / globalStats.getTotalTasks()));
    }

    private float calculateAverageTasksPerDay(List<TaskStatistics> allStats, GlobalStatistics globalStats) {
        if (allStats == null || allStats.isEmpty()) return 0f;
        LocalDateTime firstActivityDate = null;
        if (globalStats != null && globalStats.getLastActive() != null) { // Используем lastActive как дату "регистрации"
            firstActivityDate = globalStats.getLastActive(); // Это UTC
        }

        LocalDateTime firstCompletionDateTime = allStats.stream()
                .map(TaskStatistics::getCompletionTime)
                .filter(Objects::nonNull).filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        if (firstCompletionDateTime != null) {
            firstActivityDate = (firstActivityDate == null || firstCompletionDateTime.isBefore(firstActivityDate)) ?
                    firstCompletionDateTime : firstActivityDate;
        }

        if (firstActivityDate == null) return 0f;

        // Конвертируем дату первой активности в локальную для корректного расчета дней
        LocalDate startDate = dateTimeUtils.utcToLocalLocalDateTime(firstActivityDate).toLocalDate();
        long daysActive = ChronoUnit.DAYS.between(startDate, dateTimeUtils.currentLocalDate()) + 1;
        daysActive = Math.max(1, daysActive);

        long totalCompleted = allStats.stream().filter(s -> s.getCompletionTime() != null).count();
        return (float) totalCompleted / daysActive;
    }


    private DayOfWeek findMostProductiveDayOfWeek(List<TaskStatistics> allStats) {
        if (allStats == null || allStats.isEmpty()) return null;
        return aggregateTasksByDayOfWeek(allStats).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<DatePoint> mapToDatePoints(Map<LocalDate, Integer> data, LocalDate startDate, LocalDate endDate) {
        List<DatePoint> points = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            points.add(new DatePoint(date, Objects.requireNonNull(data.getOrDefault(date, 0)).floatValue()));
        }
        return points;
    }

    private List<DatePoint> mapToGamificationDatePoints(Map<LocalDate, Pair<Integer,Integer>> data, LocalDate startDate, LocalDate endDate, java.util.function.Function<Pair<Integer,Integer>, Integer> valueSelector) {
        List<DatePoint> points = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Pair<Integer,Integer> pair = data.getOrDefault(date, new Pair<>(0,0));
            points.add(new DatePoint(date, valueSelector.apply(pair).floatValue()));
        }
        return points;
    }

    private List<DayOfWeekPoint> mapToDayOfWeekPoints(Map<DayOfWeek, Integer> data) {
        List<DayOfWeekPoint> points = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) { // Итерация по всем дням недели
            points.add(new DayOfWeekPoint(day, Objects.requireNonNull(data.getOrDefault(day, 0)).floatValue()));
        }
        // Сортируем по стандартному порядку дней недели (Пн-Вс)
        points.sort(Comparator.comparingInt(p -> p.dayOfWeek.getValue()));
        return points;
    }


    // --- Методы для UI ---
    public void selectPeriod(StatsPeriod period) {
        if (period == _selectedPeriodLiveData.getValue() && period != StatsPeriod.CUSTOM) return;
        logger.debug(TAG, "Period selected via UI: " + period);
        _selectedPeriodLiveData.setValue(period); // Это вызовет triggerLoadWithDebounce
    }

    public void selectCustomDateRange(LocalDate startDate, LocalDate endDate) {
        logger.debug(TAG, "Custom date range selected: " + startDate + " - " + endDate);
        _selectedPeriodLiveData.setValue(StatsPeriod.CUSTOM);
        _customDateRangeLiveData.setValue(new Pair<>(startDate, endDate)); // Это вызовет triggerLoadWithDebounce
    }

    public void clearError() {
        updateUiState(s -> s.copy(null, null, null, null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,true));
    }

    private Pair<LocalDate, LocalDate> calculateDateRangeInternal(StatsPeriod period, LocalDate today) {
        return switch (period) {
            case MONTH -> new Pair<>(today.withDayOfMonth(1), today);
            case ALL_TIME ->
                // Для "Все время" можно взять очень раннюю дату или дату регистрации пользователя, если она есть
                // Здесь для примера - последние 365 дней
                    new Pair<>(today.minusDays(365), today);
            case CUSTOM -> {
                Pair<LocalDate, LocalDate> currentCustom = _customDateRangeLiveData.getValue();
                yield currentCustom != null ? currentCustom : new Pair<>(today.minusDays(6), today);
            }
            default -> new Pair<>(today.with(DayOfWeek.MONDAY), today);
        };
    }

    // Вспомогательный интерфейс для обновления UI State
    @FunctionalInterface
    private interface UiStateUpdaterStatistics {
        StatisticsScreenUiState update(StatisticsScreenUiState currentState);
    }
    private void updateUiState(UiStateUpdaterStatistics updater) {
        StatisticsScreenUiState current = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.update(current != null ? current : new StatisticsScreenUiState()));
    }

    // Вспомогательный класс Pair
    @Getter
    private static class Pair<F, S> {
        final F first;
        final S second;
        Pair(F f, S s) { first = f; second = s; }
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (debounceExecutor != null && !debounceExecutor.isShutdown()) {
            debounceExecutor.shutdownNow();
        }
        // Отписка от LiveData, если использовался observeForever (здесь не используется для основных потоков данных)
        logger.debug(TAG, "ViewModel cleared.");
    }
}