package com.example.projectquestonjava.feature.gamification.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.ChallengeRule;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationChallengeProgress;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

public interface ChallengeRepository {
    ListenableFuture<Challenge> getChallenge(long challengeId);
    ListenableFuture<List<ChallengeRule>> getChallengeRules(long challengeId);
    ListenableFuture<ChallengeRule> getRule(long ruleId);
    ListenableFuture<Void> updateChallengeStatus(long challengeId, ChallengeStatus status);

    ListenableFuture<GamificationChallengeProgress> getProgressForRule(long challengeId, long ruleId);
    ListenableFuture<List<GamificationChallengeProgress>> getAllProgressForChallenge(long challengeId);
    ListenableFuture<Void> insertOrUpdateProgress(GamificationChallengeProgress progress);

    LiveData<List<Challenge>> getActiveChallengesFlow();
    ListenableFuture<List<Challenge>> getActiveChallenges();
    LiveData<List<ChallengeProgressFullDetails>> getChallengesWithDetailsFlow(ChallengeStatus filterStatus);

    ListenableFuture<Void> resetPeriodicProgress(String period, LocalDateTime resetThreshold);
    ListenableFuture<Void> deleteProgressForGamification(long gamificationId);
    ListenableFuture<Void> deleteProgressForChallenge(long challengeId);
    ListenableFuture<Void> deleteRulesForChallenge(long challengeId);
}