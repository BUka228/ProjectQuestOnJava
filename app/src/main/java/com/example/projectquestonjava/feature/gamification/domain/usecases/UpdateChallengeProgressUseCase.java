package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher; // Не будет использоваться для executeSync
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.ChallengeRule;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationChallengeProgress;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import com.example.projectquestonjava.feature.gamification.domain.model.GamificationEvent;
import com.example.projectquestonjava.feature.gamification.domain.repository.ChallengeRepository; // Ожидает ...Sync методы
import com.example.projectquestonjava.feature.gamification.domain.repository.RewardRepository;   // Ожидает ...Sync методы
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
// MoreExecutors не нужен для синхронной версии
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
import java.util.concurrent.Executor; // Не используется для executeSync
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class UpdateChallengeProgressUseCase {
    private static final String TAG = "UpdateChallengeProgress";

    private final ChallengeRepository challengeRepository; // Ожидает getChallengeByIdSync, getChallengeRulesSync, getProgressForRuleSync, insertOrUpdateProgressSync, updateChallengeStatusSync
    private final RewardRepository rewardRepository;       // Ожидает getRewardByIdSync
    private final ApplyRewardUseCase applyRewardUseCase;     // Уже синхронный
    private final Logger logger;
    // ioExecutor не нужен для executeSync, если он вызывается из другого потока

    @Inject
    public UpdateChallengeProgressUseCase(
            ChallengeRepository challengeRepository,
            RewardRepository rewardRepository,
            ApplyRewardUseCase applyRewardUseCase,
            @IODispatcher Executor ioExecutor, // Оставляем для конструктора, если другие методы его используют
            Logger logger) {
        this.challengeRepository = challengeRepository;
        this.rewardRepository = rewardRepository;
        this.applyRewardUseCase = applyRewardUseCase;
        this.logger = logger;
    }

    // Новый синхронный метод для использования внутри транзакций
    public ApplyRewardUseCase.RewardApplicationResult executeSync(long gamificationId, GamificationEvent event) throws Exception {
        logger.debug(TAG, "SYNC Processing event: " + event.getClass().getSimpleName() + " for gamificationId " + gamificationId);
        AtomicInteger totalDeltaXp = new AtomicInteger(0);
        AtomicInteger totalDeltaCoins = new AtomicInteger(0);

        // Получаем активные челленджи синхронно
        List<Challenge> activeChallenges = challengeRepository.getActiveChallengesSync(); // НУЖЕН getActiveChallengesSync()
        if (activeChallenges == null) {
            logger.warn(TAG, "SYNC No active challenges found or error fetching them.");
            return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
        }
        logger.debug(TAG, "SYNC Found " + activeChallenges.size() + " active challenges.");

        for (Challenge challenge : activeChallenges) {
            List<ChallengeRule> rules = challengeRepository.getChallengeRulesSync(challenge.getId()); // НУЖЕН getChallengeRulesSync()
            if (rules == null || rules.isEmpty()) {
                continue;
            }
            for (ChallengeRule rule : rules) {
                if (isRuleApplicable(rule, event)) {
                    logger.debug(TAG, "SYNC Rule " + rule.getId() + " (Challenge " + challenge.getId() + ") matches event.");
                    try {
                        // updateProgressAndCheckCompletion теперь тоже должен быть синхронным
                        ApplyRewardUseCase.RewardApplicationResult rewardDelta =
                                updateProgressAndCheckCompletionSync(gamificationId, challenge.getId(), rule);
                        if (rewardDelta != null) {
                            totalDeltaXp.addAndGet(rewardDelta.getDeltaXp());
                            totalDeltaCoins.addAndGet(rewardDelta.getDeltaCoins());
                        }
                    } catch (Exception e) {
                        logger.error(TAG, "SYNC Error updating progress for rule " + rule.getId() + " in challenge " + challenge.getId(), e);
                        // Не прерываем весь процесс из-за одного правила, но логируем
                    }
                }
            }
        }
        logger.debug(TAG, "SYNC Finished processing event. Total Delta(XP/Coins): (" + totalDeltaXp.get() + "/" + totalDeltaCoins.get() + ")");
        return new ApplyRewardUseCase.RewardApplicationResult(totalDeltaXp.get(), totalDeltaCoins.get());
    }


    // Старый асинхронный метод, если он все еще где-то нужен (например, для вызова не из транзакции)
    // Он должен теперь использовать executeSync внутри Futures.submit
    public ListenableFuture<ApplyRewardUseCase.RewardApplicationResult> execute(long gamificationId, GamificationEvent event, Executor executor) {
        return Futures.submit(() -> executeSync(gamificationId, event), executor);
    }


    private ApplyRewardUseCase.RewardApplicationResult updateProgressAndCheckCompletionSync(long gamificationId, long challengeId, ChallengeRule rule) throws Exception {
        GamificationChallengeProgress progress = challengeRepository.getProgressForRuleSync(challengeId, rule.getId()); // НУЖЕН getProgressForRuleSync()

        if (progress != null && progress.isCompleted() && isProgressValidForPeriod(progress, rule)) {
            logger.debug(TAG, "SYNC Rule " + rule.getId() + " (Challenge " + challengeId + ") already completed for this period.");
            return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
        }

        int currentProgressValue = (progress != null && isProgressValidForPeriod(progress, rule)) ? progress.getProgress() : 0;
        int newProgressValue = currentProgressValue + 1;
        boolean isRuleCompletedNow = newProgressValue >= rule.getTarget();

        GamificationChallengeProgress progressToUpdate = new GamificationChallengeProgress(
                gamificationId, challengeId, rule.getId(), newProgressValue, isRuleCompletedNow, LocalDateTime.now()
        );

        challengeRepository.insertOrUpdateProgressSync(progressToUpdate); // НУЖЕН insertOrUpdateProgressSync()
        logger.debug(TAG, "SYNC Progress updated for rule " + rule.getId() + ". New value: " + newProgressValue + ", Completed: " + isRuleCompletedNow);

        if (isRuleCompletedNow) {
            return checkOverallChallengeCompletionSync(gamificationId, challengeId);
        } else {
            return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
        }
    }

    private ApplyRewardUseCase.RewardApplicationResult checkOverallChallengeCompletionSync(long gamificationId, long challengeId) throws Exception {
        List<ChallengeRule> allRules = challengeRepository.getChallengeRulesSync(challengeId); // SYNC
        List<GamificationChallengeProgress> allProgressList = challengeRepository.getAllProgressForChallengeSync(gamificationId, challengeId); // НУЖЕН getAllProgressForChallengeSync

        if (allRules == null || allRules.isEmpty()) {
            logger.warn(TAG, "SYNC No rules found for challenge " + challengeId + ". Cannot complete.");
            return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
        }

        Map<Long, GamificationChallengeProgress> progressMap = allProgressList.stream()
                .collect(Collectors.toMap(GamificationChallengeProgress::getRuleId, p -> p, (p1, p2) -> p1));

        boolean allRulesAreCompletedThisPeriod = allRules.stream().allMatch(rule -> {
            GamificationChallengeProgress progressForRule = progressMap.get(rule.getId());
            return progressForRule != null && progressForRule.isCompleted() && isProgressValidForPeriod(progressForRule, rule);
        });

        if (allRulesAreCompletedThisPeriod) {
            logger.info(TAG, "SYNC Challenge " + challengeId + " completed by user " + gamificationId + "!");
            challengeRepository.updateChallengeStatusSync(challengeId, ChallengeStatus.COMPLETED); // НУЖЕН updateChallengeStatusSync
            Challenge challenge = challengeRepository.getChallengeByIdSync(challengeId); // НУЖЕН getChallengeByIdSync
            if (challenge == null) throw new IllegalStateException("Challenge " + challengeId + " not found after completion.");
            Reward reward = rewardRepository.getRewardByIdSync(challenge.getRewardId()); // НУЖЕН getRewardByIdSync
            if (reward == null) throw new IllegalStateException("Reward " + challenge.getRewardId() + " not found.");

            ApplyRewardUseCase.RewardApplicationResult rewardDelta = applyRewardUseCase.execute(gamificationId, reward); // applyRewardUseCase уже синхронный
            logger.info(TAG, "SYNC Awarded reward '" + reward.getName() + "'. Delta: " + rewardDelta);
            return rewardDelta;
        }
        logger.debug(TAG, "SYNC Challenge " + challengeId + " not yet fully completed for this period.");
        return new ApplyRewardUseCase.RewardApplicationResult(0, 0);
    }

    // Методы isRuleApplicable, checkRuleConditions, isProgressValidForPeriod остаются без изменений,
    // так как они не взаимодействуют с БД напрямую.
    private boolean isRuleApplicable(ChallengeRule rule, GamificationEvent event) {
        // ... (без изменений) ...
        boolean typeMatches = false;
        if (event instanceof GamificationEvent.TaskCompleted && rule.getType() == ChallengeType.TASK_COMPLETION) {
            typeMatches = true;
        } else if (event instanceof GamificationEvent.PomodoroCompleted && rule.getType() == ChallengeType.POMODORO_SESSION) {
            typeMatches = true;
        } else if (event instanceof GamificationEvent.StreakUpdated && rule.getType() == ChallengeType.DAILY_STREAK) {
            typeMatches = true;
        }
        if (!typeMatches) return false;
        return checkRuleConditions(rule, event);
    }

    private boolean checkRuleConditions(ChallengeRule rule, GamificationEvent event) {
        // ... (без изменений) ...
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
            return true;
        } catch (JSONException e) {
            logger.error(TAG, "Error parsing conditions JSON for rule " + rule.getId() + ": " + rule.getConditionJson(), e);
            return false;
        }
    }
    private boolean isProgressValidForPeriod(GamificationChallengeProgress progress, ChallengeRule rule) {
        if (rule.getPeriod() == ChallengePeriod.ONCE || rule.getPeriod() == ChallengePeriod.EVENT) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdated = progress.getLastUpdated();
        LocalDate nowDateUtc = now.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
        LocalDate lastUpdateDateUtc = lastUpdated.toLocalDate();

        switch (rule.getPeriod()) {
            case DAILY:
                return lastUpdateDateUtc.isEqual(nowDateUtc);
            case WEEKLY:
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                return lastUpdated.getYear() == now.getYear() &&
                        lastUpdated.get(weekFields.weekOfWeekBasedYear()) == now.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).get(weekFields.weekOfWeekBasedYear());
            case MONTHLY:
                return lastUpdated.getYear() == now.getYear() &&
                        lastUpdated.getMonth() == now.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).getMonth();
            default:
                return true;
        }
    }
}