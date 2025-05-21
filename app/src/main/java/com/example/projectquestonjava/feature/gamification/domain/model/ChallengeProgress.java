package com.example.projectquestonjava.feature.gamification.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;

@Getter
public class ChallengeProgress {
    private final long challengeId;
    private final String challengeName;
    private final String challengeDescription;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final ChallengeStatus status;
    private final long rewardId;
    private final int progress;
    private final int target;
    private final ChallengePeriod period;

    public ChallengeProgress(long challengeId, String challengeName, String challengeDescription, LocalDateTime startDate, LocalDateTime endDate, ChallengeStatus status, long rewardId, int progress, int target, ChallengePeriod period) {
        this.challengeId = challengeId;
        this.challengeName = challengeName;
        this.challengeDescription = challengeDescription;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.rewardId = rewardId;
        this.progress = progress;
        this.target = target;
        this.period = period;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChallengeProgress that = (ChallengeProgress) o;
        return challengeId == that.challengeId && rewardId == that.rewardId && progress == that.progress && target == that.target && Objects.equals(challengeName, that.challengeName) && Objects.equals(challengeDescription, that.challengeDescription) && Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate) && status == that.status && period == that.period;
    }

    @Override
    public int hashCode() {
        return Objects.hash(challengeId, challengeName, challengeDescription, startDate, endDate, status, rewardId, progress, target, period);
    }
}