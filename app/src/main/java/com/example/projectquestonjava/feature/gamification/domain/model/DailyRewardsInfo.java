package com.example.projectquestonjava.feature.gamification.domain.model;

import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

@Getter
public class DailyRewardsInfo {
    private final List<Reward> rewards;
    private final int currentStreak;
    private final boolean canClaimToday;
    private final int todayStreakDay;
    private final long daysSinceLastClaim;

    public DailyRewardsInfo(List<Reward> rewards, int currentStreak, boolean canClaimToday, int todayStreakDay, long daysSinceLastClaim) {
        this.rewards = rewards;
        this.currentStreak = currentStreak;
        this.canClaimToday = canClaimToday;
        this.todayStreakDay = todayStreakDay;
        this.daysSinceLastClaim = daysSinceLastClaim;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyRewardsInfo that = (DailyRewardsInfo) o;
        return currentStreak == that.currentStreak && canClaimToday == that.canClaimToday && todayStreakDay == that.todayStreakDay && daysSinceLastClaim == that.daysSinceLastClaim && Objects.equals(rewards, that.rewards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rewards, currentStreak, canClaimToday, todayStreakDay, daysSinceLastClaim);
    }
}