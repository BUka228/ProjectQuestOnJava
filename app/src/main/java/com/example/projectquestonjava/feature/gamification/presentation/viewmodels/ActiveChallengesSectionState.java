package com.example.projectquestonjava.feature.gamification.presentation.viewmodels; // или другой подходящий пакет

import java.util.Collections;
import java.util.List;

import lombok.Data;


public record ActiveChallengesSectionState(int totalActiveCount,
                                           List<ChallengeCardInfo> allActiveChallengesInfo,
                                           int dailyCompletedCount, int dailyTotalCount,
                                           int weeklyCompletedCount, int weeklyTotalCount,
                                           int urgentCount, int nearCompletionCount) {

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

}