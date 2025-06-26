package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeCardInfo; // Убедитесь, что он импортирован
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Если это не Java 14+ record, а обычный класс:
public class ActiveChallengesSectionState {
    private final int totalActiveCount;
    private final List<ChallengeCardInfo> allActiveChallengesInfo;
    private final int dailyCompletedCount;
    private final int dailyTotalCount;
    private final int weeklyCompletedCount;
    private final int weeklyTotalCount;
    private final int urgentCount;
    private final int nearCompletionCount;

    public ActiveChallengesSectionState(int totalActiveCount, List<ChallengeCardInfo> allActiveChallengesInfo,
                                        int dailyCompletedCount, int dailyTotalCount,
                                        int weeklyCompletedCount, int weeklyTotalCount,
                                        int urgentCount, int nearCompletionCount) {
        this.totalActiveCount = totalActiveCount;
        this.allActiveChallengesInfo = allActiveChallengesInfo != null ? allActiveChallengesInfo : Collections.emptyList();
        this.dailyCompletedCount = dailyCompletedCount;
        this.dailyTotalCount = dailyTotalCount;
        this.weeklyCompletedCount = weeklyCompletedCount;
        this.weeklyTotalCount = weeklyTotalCount;
        this.urgentCount = urgentCount;
        this.nearCompletionCount = nearCompletionCount;
    }

    // Конструктор по умолчанию, если нужен
    public ActiveChallengesSectionState() {
        this(0, Collections.emptyList(), 0, 0, 0, 0, 0, 0);
    }

    // Геттеры
    public int getTotalActiveCount() {
        return totalActiveCount;
    }

    public List<ChallengeCardInfo> getAllActiveChallengesInfo() {
        return allActiveChallengesInfo;
    }

    public int getDailyCompletedCount() {
        return dailyCompletedCount;
    }

    public int getDailyTotalCount() {
        return dailyTotalCount;
    }

    public String getDailyProgressText() {
        return dailyTotalCount > 0 ? dailyCompletedCount + "/" + dailyTotalCount : null;
    }

    public int getWeeklyCompletedCount() {
        return weeklyCompletedCount;
    }

    public int getWeeklyTotalCount() {
        return weeklyTotalCount;
    }

    public String getWeeklyProgressText() {
        return weeklyTotalCount > 0 ? weeklyCompletedCount + "/" + weeklyTotalCount : null;
    }

    public int getUrgentCount() {
        return urgentCount;
    }

    public int getNearCompletionCount() {
        return nearCompletionCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveChallengesSectionState that = (ActiveChallengesSectionState) o;
        return totalActiveCount == that.totalActiveCount &&
                dailyCompletedCount == that.dailyCompletedCount &&
                dailyTotalCount == that.dailyTotalCount &&
                weeklyCompletedCount == that.weeklyCompletedCount &&
                weeklyTotalCount == that.weeklyTotalCount &&
                urgentCount == that.urgentCount &&
                nearCompletionCount == that.nearCompletionCount &&
                Objects.equals(allActiveChallengesInfo, that.allActiveChallengesInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalActiveCount, allActiveChallengesInfo, dailyCompletedCount, dailyTotalCount,
                weeklyCompletedCount, weeklyTotalCount, urgentCount, nearCompletionCount);
    }
}