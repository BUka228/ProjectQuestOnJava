package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.ChallengeRule;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationChallengeProgress;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import com.example.projectquestonjava.feature.gamification.domain.model.GamificationEvent;
import com.example.projectquestonjava.feature.gamification.domain.repository.ChallengeRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.RewardRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class UpdateChallengeProgressUseCase {
    private static final String TAG = "UpdateChallengeProgress";

    private final ChallengeRepository challengeRepository;
    private final RewardRepository rewardRepository;
    private final ApplyRewardUseCase applyRewardUseCase;
    private final Logger logger;

    @Inject
    public UpdateChallengeProgressUseCase(
            ChallengeRepository challengeRepository,
            RewardRepository rewardRepository,
            ApplyRewardUseCase applyRewardUseCase,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.challengeRepository = challengeRepository;
        this.rewardRepository = rewardRepository;
        this.applyRewardUseCase = applyRewardUseCase;
        this.logger = logger;
    }

    public ApplyRewardUseCase.RewardApplicationResult executeSync(long gamificationId, GamificationEvent event) throws Exception {
        logger.debug(TAG, "SYNC Processing event: " + event.getClass().getSimpleName() + " for gamificationId " + gamificationId);
        AtomicInteger totalDeltaXp = new AtomicInteger(0);
        AtomicInteger totalDeltaCoins = new AtomicInteger(0);

        List<Challenge> activeChallenges = challengeRepository.getActiveChallengesSync();
        if (activeChallenges == null) {
            logger.warn(TAG, "SYNC No active challenges found or error fetching them.");
            return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
        }
        logger.debug(TAG, "SYNC Found " + activeChallenges.size() + " active challenges.");

        for (Challenge challenge : activeChallenges) {
            logger.debug(TAG, "SYNC Checking challenge: " + challenge.getName() + " (ID: " + challenge.getId() + ")");
            List<ChallengeRule> rules = challengeRepository.getChallengeRulesSync(challenge.getId());
            if (rules == null || rules.isEmpty()) {
                logger.debug(TAG, "SYNC No rules for challenge " + challenge.getId());
                continue;
            }
            for (ChallengeRule rule : rules) {
                logger.debug(TAG, "SYNC Checking rule: " + rule.getId() + " (Type: " + rule.getType() + ")");
                if (isRuleApplicable(rule, event)) {
                    logger.info(TAG, "SYNC Rule " + rule.getId() + " (Challenge " + challenge.getId() + ") MATCHES event.");
                    try {
                        ApplyRewardUseCase.RewardApplicationResult rewardDelta =
                                updateProgressAndCheckCompletionSync(gamificationId, challenge.getId(), rule);
                        if (rewardDelta != null) {
                            totalDeltaXp.addAndGet(rewardDelta.getDeltaXp());
                            totalDeltaCoins.addAndGet(rewardDelta.getDeltaCoins());
                        }
                    } catch (Exception e) {
                        logger.error(TAG, "SYNC Error updating progress for rule " + rule.getId() + " in challenge " + challenge.getId(), e);
                    }
                } else {
                    logger.debug(TAG, "SYNC Rule " + rule.getId() + " DOES NOT match event.");
                }
            }
        }
        logger.info(TAG, "SYNC Finished processing event. Total Delta(XP/Coins): (" + totalDeltaXp.get() + "/" + totalDeltaCoins.get() + ")");
        return new ApplyRewardUseCase.RewardApplicationResult(totalDeltaXp.get(), totalDeltaCoins.get());
    }

    public ListenableFuture<ApplyRewardUseCase.RewardApplicationResult> execute(long gamificationId, GamificationEvent event, Executor executor) {
        return Futures.submit(() -> executeSync(gamificationId, event), executor);
    }


    private ApplyRewardUseCase.RewardApplicationResult updateProgressAndCheckCompletionSync(long gamificationId, long challengeId, ChallengeRule rule) throws Exception {
        GamificationChallengeProgress progress = challengeRepository.getProgressForRuleSync(challengeId, rule.getId());
        logger.info(TAG, "SYNC updateProgressAndCheckCompletionSync for rule " + rule.getId() + 
                   ". Current progress: " + (progress != null ? progress.getProgress() : "null") + 
                   ", Completed: " + (progress != null && progress.isCompleted()) +
                   ", Target: " + rule.getTarget() +
                   ", Period: " + rule.getPeriod());

        // Проверяем, завершено ли правило для текущего периода
        boolean isAlreadyCompletedThisPeriod = progress != null && progress.isCompleted() && isProgressValidForPeriod(progress, rule);
        logger.info(TAG, "SYNC Rule " + rule.getId() + " isAlreadyCompletedThisPeriod: " + isAlreadyCompletedThisPeriod + 
                   " (isProgressValidForPeriod: " + (progress != null ? isProgressValidForPeriod(progress, rule) : "N/A") + ")");

        if (isAlreadyCompletedThisPeriod) {
            logger.info(TAG, "SYNC Rule " + rule.getId() + " (Challenge " + challengeId + ") already completed for this period. No update.");
            return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
        }

        // Определяем текущий прогресс
        boolean isProgressValid = progress != null && isProgressValidForPeriod(progress, rule);
        int currentProgressValue = isProgressValid ? progress.getProgress() : 0;
        logger.info(TAG, "SYNC Rule " + rule.getId() + " currentProgressValue: " + currentProgressValue + 
                   " (isProgressValid: " + isProgressValid + ")");

        int newProgressValue = currentProgressValue + 1;
        boolean isRuleCompletedNow = newProgressValue >= rule.getTarget();

        GamificationChallengeProgress progressToUpdate = new GamificationChallengeProgress(
                gamificationId, challengeId, rule.getId(), newProgressValue, isRuleCompletedNow, LocalDateTime.now()
        );

        challengeRepository.insertOrUpdateProgressSync(progressToUpdate);
        logger.info(TAG, "SYNC Progress updated for rule " + rule.getId() + ". New value: " + newProgressValue + 
                   "/" + rule.getTarget() + ", Completed: " + isRuleCompletedNow);

        if (isRuleCompletedNow) {
            logger.info(TAG, "SYNC Rule " + rule.getId() + " completed. Checking overall challenge " + challengeId + " completion.");
            return checkOverallChallengeCompletionSync(gamificationId, challengeId);
        } else {
            return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
        }
    }

    private ApplyRewardUseCase.RewardApplicationResult checkOverallChallengeCompletionSync(long gamificationId, long challengeId) throws Exception {
        List<ChallengeRule> allRules = challengeRepository.getChallengeRulesSync(challengeId);
        List<GamificationChallengeProgress> allProgressList = challengeRepository.getAllProgressForChallengeSync(gamificationId, challengeId);

        if (allRules == null || allRules.isEmpty()) {
            logger.warn(TAG, "SYNC No rules found for challenge " + challengeId + ". Cannot complete.");
            return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
        }

        Map<Long, GamificationChallengeProgress> progressMap = allProgressList.stream()
                .collect(Collectors.toMap(GamificationChallengeProgress::getRuleId, p -> p, (p1, p2) -> p1));

        boolean allRulesAreCompletedThisPeriod = allRules.stream().allMatch(rule -> {
            GamificationChallengeProgress progressForRule = progressMap.get(rule.getId());
            boolean ruleCompleted = progressForRule != null && progressForRule.isCompleted();
            
            // Для периодических челленджей дополнительно проверяем период
            if (ruleCompleted && (rule.getPeriod() == ChallengePeriod.DAILY || 
                                 rule.getPeriod() == ChallengePeriod.WEEKLY || 
                                 rule.getPeriod() == ChallengePeriod.MONTHLY)) {
                ruleCompleted = isProgressValidForPeriod(progressForRule, rule);
            }
            
            logger.debug(TAG, "SYNC Challenge " + challengeId + ", Rule " + rule.getId() + ": isCompletedThisPeriod = " + ruleCompleted + 
                         " (progress: " + (progressForRule != null ? progressForRule.getProgress() : "null") + 
                         ", completed: " + (progressForRule != null && progressForRule.isCompleted()) + ")");
            return ruleCompleted;
        });

        if (allRulesAreCompletedThisPeriod) {
            logger.info(TAG, "SYNC Challenge " + challengeId + " ALL RULES COMPLETED by user " + gamificationId + "!");
            
            Challenge challenge = challengeRepository.getChallengeByIdSync(challengeId);
            if (challenge == null) throw new IllegalStateException("Challenge " + challengeId + " not found after completion.");
            
            logger.info(TAG, "SYNC Challenge " + challengeId + " details: name='" + challenge.getName() + 
                       "', period=" + challenge.getPeriod() + ", rewardId=" + challenge.getRewardId());
            
            // Для ежедневных, недельных и месячных челленджей НЕ меняем статус на COMPLETED
            // Они должны оставаться ACTIVE для повторного выполнения в следующем периоде
            if (challenge.getPeriod() == ChallengePeriod.ONCE || challenge.getPeriod() == ChallengePeriod.EVENT) {
                challengeRepository.updateChallengeStatusSync(challengeId, ChallengeStatus.COMPLETED);
                logger.info(TAG, "SYNC Challenge " + challengeId + " status set to COMPLETED (ONCE/EVENT challenge)");
            } else {
                logger.info(TAG, "SYNC Challenge " + challengeId + " status remains ACTIVE (periodic challenge)");
            }
            
            Reward reward = rewardRepository.getRewardByIdSync(challenge.getRewardId());
            if (reward == null) throw new IllegalStateException("Reward " + challenge.getRewardId() + " not found.");
            
            logger.info(TAG, "SYNC Applying reward: " + reward.getName() + " (Type: " + reward.getRewardType() + ", Value: " + reward.getRewardValue() + ")");

            ApplyRewardUseCase.RewardApplicationResult rewardDelta = applyRewardUseCase.execute(gamificationId, reward);
            logger.info(TAG, "SYNC Awarded reward '" + reward.getName() + "' for challenge " + challengeId + ". Delta: XP=" + rewardDelta.getDeltaXp() + ", Coins=" + rewardDelta.getDeltaCoins());
            return rewardDelta;
        }
        logger.debug(TAG, "SYNC Challenge " + challengeId + " not yet fully completed for this period.");
        return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
    }

    private boolean isRuleApplicable(ChallengeRule rule, GamificationEvent event) {
        boolean typeMatches = false;
        if (event instanceof GamificationEvent.TaskCompleted && rule.getType() == ChallengeType.TASK_COMPLETION) {
            typeMatches = true;
        } else if (event instanceof GamificationEvent.PomodoroCompleted && rule.getType() == ChallengeType.POMODORO_SESSION) {
            typeMatches = true;
        } else if (event instanceof GamificationEvent.StreakUpdated && rule.getType() == ChallengeType.DAILY_STREAK) {
            typeMatches = true;
        }
        // Добавьте другие типы событий и правил по мере необходимости
        if (!typeMatches) {
            logger.debug(TAG, "SYNC Rule " + rule.getId() + " type " + rule.getType() + " does not match event type " + event.getClass().getSimpleName());
            return false;
        }
        boolean conditionsMet = checkRuleConditions(rule, event);
        logger.debug(TAG, "SYNC Rule " + rule.getId() + " conditionsMet: " + conditionsMet);
        return conditionsMet;
    }

    private boolean checkRuleConditions(ChallengeRule rule, GamificationEvent event) {
        if (rule.getConditionJson() == null || rule.getConditionJson().trim().isEmpty()) return true;
        try {
            JSONObject conditions = new JSONObject(rule.getConditionJson());
            if (event instanceof GamificationEvent.TaskCompleted) {
                GamificationEvent.TaskCompleted taskEvent = (GamificationEvent.TaskCompleted) event;
                if (conditions.has("tags")) {
                    JSONArray requiredTagsJson = conditions.getJSONArray("tags");
                    Set<String> taskTagNames = taskEvent.getTags().stream()
                            .map(tag -> tag.getName().toLowerCase())
                            .collect(Collectors.toSet());
                    for (int i = 0; i < requiredTagsJson.length(); i++) {
                        if (!taskTagNames.contains(requiredTagsJson.getString(i).toLowerCase())) {
                            return false;
                        }
                    }
                }
                // Добавьте другие условия для TaskCompleted, если нужно
            } else if (event instanceof GamificationEvent.PomodoroCompleted) {
                GamificationEvent.PomodoroCompleted pomodoroEvent = (GamificationEvent.PomodoroCompleted) event;
                if (conditions.has("minDurationMinutes")) {
                    if (pomodoroEvent.getDurationSeconds() < conditions.getInt("minDurationMinutes") * 60) {
                        return false;
                    }
                }
            } else if (event instanceof GamificationEvent.StreakUpdated) {
                GamificationEvent.StreakUpdated streakEvent = (GamificationEvent.StreakUpdated) event;
                if (conditions.has("minStreak")) {
                    if (streakEvent.getNewStreakValue() < conditions.getInt("minStreak")) return false;
                }
                if (conditions.has("exactStreak")) {
                    if (streakEvent.getNewStreakValue() != conditions.getInt("exactStreak")) return false;
                }
            }
            // Добавьте другие типы событий
            return true;
        } catch (JSONException e) {
            logger.error(TAG, "Error parsing conditions JSON for rule " + rule.getId() + ": " + rule.getConditionJson(), e);
            return false;
        }
    }
    private boolean isProgressValidForPeriod(GamificationChallengeProgress progress, ChallengeRule rule) {
        if (rule.getPeriod() == ChallengePeriod.ONCE || rule.getPeriod() == ChallengePeriod.EVENT) {
            return true; // Для этих периодов прогресс не сбрасывается автоматически датой
        }
        LocalDateTime now = LocalDateTime.now(); // Локальное время устройства
        LocalDateTime lastUpdated = progress.getLastUpdated(); // Время из БД

        logger.info(TAG, "SYNC isProgressValidForPeriod check: rule=" + rule.getId() + 
                   ", period=" + rule.getPeriod() + 
                   ", now=" + now + 
                   ", lastUpdated=" + lastUpdated);

        // Конвертируем lastUpdated в локальную зону для сравнения дат
        LocalDateTime lastUpdatedLocal = lastUpdated.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneOffset.systemDefault())
                .toLocalDateTime();
        LocalDate nowDateLocal = now.toLocalDate();
        LocalDate lastUpdateDateLocal = lastUpdatedLocal.toLocalDate();

        logger.info(TAG, "SYNC Date comparison: nowDate=" + nowDateLocal + 
                   ", lastUpdateDate=" + lastUpdateDateLocal + 
                   " (converted from " + lastUpdated + " to " + lastUpdatedLocal + ")");

        boolean result;
        switch (rule.getPeriod()) {
            case DAILY:
                // Для ежедневных челленджей прогресс валиден только если он был обновлен сегодня
                result = lastUpdateDateLocal.isEqual(nowDateLocal);
                break;
            case WEEKLY:
                WeekFields weekFields = WeekFields.of(Locale.getDefault()); // Или конкретная локаль
                result = lastUpdatedLocal.getYear() == now.getYear() &&
                        lastUpdatedLocal.get(weekFields.weekOfWeekBasedYear()) == now.get(weekFields.weekOfWeekBasedYear());
                break;
            case MONTHLY:
                result = lastUpdatedLocal.getYear() == now.getYear() &&
                        lastUpdatedLocal.getMonth() == now.getMonth();
                break;
            default:
                result = true; // Неизвестный или необрабатываемый период
        }
        
        logger.info(TAG, "SYNC isProgressValidForPeriod result: " + result);
        return result;
    }
}