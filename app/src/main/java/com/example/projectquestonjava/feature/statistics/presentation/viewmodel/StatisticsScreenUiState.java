package com.example.projectquestonjava.feature.statistics.presentation.viewmodel;

import androidx.annotation.Nullable;
import com.example.projectquestonjava.feature.statistics.data.model.GlobalStatistics;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value // Делает класс immutable, генерирует геттеры, equals, hashCode, toString
@Builder(toBuilder = true) // Позволяет легко создавать копии с изменениями через builder
public class StatisticsScreenUiState {
    @With StatsPeriod selectedPeriod;
    @With boolean isLoading;
    @With @Nullable String error;
    @With LocalDate selectedStartDate;
    @With LocalDate selectedEndDate;

    // Сводная статистика
    @With @Nullable GlobalStatistics globalStats;
    @With @Nullable Float taskCompletionRateOverall;
    @With float averageTasksPerDayOverall;
    @With @Nullable DayOfWeek mostProductiveDayOfWeekOverall;

    // Данные за выбранный период для графиков
    @With List<DatePoint> taskCompletionTrend;
    @With List<DatePoint> pomodoroFocusTrend;
    @With List<DatePoint> xpGainTrend;
    @With List<DatePoint> coinGainTrend;
    @With List<DayOfWeekPoint> tasksCompletedByDayOfWeek;

    // Метрики за выбранный период
    @With int totalTasksCompletedInPeriod;
    @With int totalPomodoroMinutesInPeriod;
    @With float averageDailyPomodoroMinutes;
    @With int totalXpGainedInPeriod;
    @With int totalCoinsGainedInPeriod;
    @With @Nullable LocalDate mostProductiveDayInPeriod;

    // Конструктор по умолчанию для HiltViewModel или первоначальной инициализации
    public static StatisticsScreenUiState createDefault() {
        LocalDate today = LocalDate.now();
        return StatisticsScreenUiState.builder()
                .selectedPeriod(StatsPeriod.WEEK)
                .isLoading(true)
                .error(null)
                .selectedStartDate(today.with(DayOfWeek.MONDAY))
                .selectedEndDate(today)
                .globalStats(null)
                .taskCompletionRateOverall(null)
                .averageTasksPerDayOverall(0f)
                .mostProductiveDayOfWeekOverall(null)
                .taskCompletionTrend(Collections.emptyList())
                .pomodoroFocusTrend(Collections.emptyList())
                .xpGainTrend(Collections.emptyList())
                .coinGainTrend(Collections.emptyList())
                .tasksCompletedByDayOfWeek(defaultDayOfWeekPoints())
                .totalTasksCompletedInPeriod(0)
                .totalPomodoroMinutesInPeriod(0)
                .averageDailyPomodoroMinutes(0f)
                .totalXpGainedInPeriod(0)
                .totalCoinsGainedInPeriod(0)
                .mostProductiveDayInPeriod(null)
                .build();
    }

    private static List<DayOfWeekPoint> defaultDayOfWeekPoints() {
        List<DayOfWeekPoint> defaults = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            defaults.add(new DayOfWeekPoint(day, 0f));
        }
        defaults.sort(Comparator.comparingInt(p -> p.dayOfWeek().getValue()));
        return Collections.unmodifiableList(defaults);
    }
}