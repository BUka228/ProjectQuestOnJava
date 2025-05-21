package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.data.model.core.Tag;
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
import com.google.common.util.concurrent.MoreExecutors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger; // Для атомарных счетчиков
import java.util.stream.Collectors;
import javax.inject.Inject;

public class UpdateChallengeProgressUseCase {
    private static final String TAG = "UpdateChallengeProgress";

    private final ChallengeRepository challengeRepository;
    private final RewardRepository rewardRepository;
    private final ApplyRewardUseCase applyRewardUseCase;
    private final Executor ioExecutor;
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
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<ApplyRewardUseCase.RewardApplicationResult> execute(long gamificationId, GamificationEvent event) {
        return Futures.submitAsync(() -> { // Вся логика в submitAsync
            logger.debug(TAG, "Processing event: " + event.getClass().getSimpleName() + " for gamificationId " + gamificationId);

            AtomicInteger totalDeltaXp = new AtomicInteger(0); // Используем AtomicInteger для потокобезопасного сложения
            AtomicInteger totalDeltaCoins = new AtomicInteger(0);

            try {
                List<Challenge> activeChallenges = Futures.getDone(challengeRepository.getActiveChallenges());
                logger.debug(TAG, "Found " + activeChallenges.size() + " active challenges for user.");

                List<ListenableFuture<Void>> allChallengeProcessingFutures = new ArrayList<>();

                for (Challenge challenge : activeChallenges) {
                    ListenableFuture<Void> singleChallengeFuture = Futures.transformAsync(
                            challengeRepository.getChallengeRules(challenge.getId()),
                            rules -> {
                                if (rules == null || rules.isEmpty()) {
                                    return Futures.immediateFuture(null);
                                }
                                List<ListenableFuture<Void>> ruleFutures = new ArrayList<>();
                                for (ChallengeRule rule : rules) {
                                    if (isRuleApplicable(rule, event)) {
                                        logger.debug(TAG, "Rule " + rule.getId() + " (Challenge " + challenge.getId() + ") matches event.");
                                        ListenableFuture<ApplyRewardUseCase.RewardApplicationResult> updateResultFuture =
                                                updateProgressAndCheckCompletion(gamificationId, challenge.getId(), rule);

                                        ruleFutures.add(Futures.transform(updateResultFuture, rewardDelta -> {
                                            if (rewardDelta != null) {
                                                totalDeltaXp.addAndGet(rewardDelta.getDeltaXp());
                                                totalDeltaCoins.addAndGet(rewardDelta.getDeltaCoins());
                                            }
                                            return null;
                                        }, MoreExecutors.directExecutor()));
                                    }
                                }
                                return Futures.whenAllSucceed(ruleFutures).call(() -> null, MoreExecutors.directExecutor());
                            },
                            ioExecutor // Executor для transformAsync правил
                    );
                    allChallengeProcessingFutures.add(singleChallengeFuture);
                }

                // Ждем завершения обработки всех челленджей
                return Futures.whenAllSucceed(allChallengeProcessingFutures).call(() -> {
                    logger.debug(TAG, "Finished processing event for challenges. Total Delta(XP/Coins): (" + totalDeltaXp.get() + "/" + totalDeltaCoins.get() + ")");
                    return new ApplyRewardUseCase.RewardApplicationResult(totalDeltaXp.get(), totalDeltaCoins.get());
                }, MoreExecutors.directExecutor());

            } catch (Exception e) {
                logger.error(TAG, "Critical error processing challenges for event " + event, e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor); // submitAsync на ioExecutor
    }

    private ListenableFuture<ApplyRewardUseCase.RewardApplicationResult> updateProgressAndCheckCompletion(long gamificationId, long challengeId, ChallengeRule rule) {
        return Futures.transformAsync(
                challengeRepository.getProgressForRule(challengeId, rule.getId()),
                progress -> {
                    if (progress != null && progress.isCompleted() && isProgressValidForPeriod(progress, rule)) {
                        logger.debug(TAG, "Rule " + rule.getId() + " (Challenge " + challengeId + ") already completed for this period.");
                        return Futures.immediateFuture(new ApplyRewardUseCase.RewardApplicationResult(0, 0));
                    }

                    int currentProgressValue = (progress != null && isProgressValidForPeriod(progress, rule)) ? progress.getProgress() : 0;
                    int newProgressValue = currentProgressValue + 1;
                    boolean isRuleCompletedNow = newProgressValue >= rule.getTarget();

                    GamificationChallengeProgress progressToUpdate = new GamificationChallengeProgress(
                            gamificationId, challengeId, rule.getId(), newProgressValue, isRuleCompletedNow, LocalDateTime.now()
                    );

                    return Futures.transformAsync(
                            challengeRepository.insertOrUpdateProgress(progressToUpdate),
                            aVoid -> {
                                logger.debug(TAG, "Progress updated for rule " + rule.getId() + ". New value: " + newProgressValue + ", Completed: " + isRuleCompletedNow);
                                if (isRuleCompletedNow) {
                                    return checkOverallChallengeCompletion(gamificationId, challengeId);
                                } else {
                                    return Futures.immediateFuture(new ApplyRewardUseCase.RewardApplicationResult(0, 0));
                                }
                            },
                            ioExecutor
                    );
                },
                ioExecutor
        );
    }

    private ListenableFuture<ApplyRewardUseCase.RewardApplicationResult> checkOverallChallengeCompletion(long gamificationId, long challengeId) {
        ListenableFuture<List<ChallengeRule>> allRulesFuture = challengeRepository.getChallengeRules(challengeId);
        ListenableFuture<List<GamificationChallengeProgress>> allProgressFuture = challengeRepository.getAllProgressForChallenge(challengeId);

        ListenableFuture<List<Object>> combined = Futures.allAsList(allRulesFuture, allProgressFuture);

        return Futures.transformAsync(combined, results -> {
            List<ChallengeRule> allRules = (List<ChallengeRule>) results.get(0);
            List<GamificationChallengeProgress> allProgressList = (List<GamificationChallengeProgress>) results.get(1);

            if (allRules == null || allRules.isEmpty()) {
                logger.warn(TAG, "No rules found for challenge " + challengeId + " during completion check. Cannot complete.");
                return Futures.immediateFuture(new ApplyRewardUseCase.RewardApplicationResult(0, 0));
            }

            Map<Long, GamificationChallengeProgress> progressMap = allProgressList.stream()
                    .collect(Collectors.toMap(GamificationChallengeProgress::getRuleId, p -> p, (p1, p2) -> p1)); // В случае дубликатов взять первый

            boolean allRulesAreCompletedThisPeriod = allRules.stream().allMatch(rule -> {
                GamificationChallengeProgress progressForRule = progressMap.get(rule.getId());
                return progressForRule != null && progressForRule.isCompleted() && isProgressValidForPeriod(progressForRule, rule);
            });

            if (allRulesAreCompletedThisPeriod) {
                logger.info(TAG, "Challenge " + challengeId + " completed by user " + gamificationId + "!");
                // 1. Обновляем статус челленджа
                return Futures.transformAsync(challengeRepository.updateChallengeStatus(challengeId, ChallengeStatus.COMPLETED),
                        aVoidStatus -> Futures.transformAsync(challengeRepository.getChallenge(challengeId),
                                challenge -> {
                                    if (challenge == null) throw new IllegalStateException("Challenge " + challengeId + " not found after completion check.");
                                    return Futures.transformAsync(rewardRepository.getRewardById(challenge.getRewardId()),
                                            reward -> {
                                                if (reward == null) throw new IllegalStateException("Reward " + challenge.getRewardId() + " not found for challenge " + challengeId);
                                                // 3. Применяем награду
                                                return Futures.transform(applyRewardUseCase.execute(gamificationId, reward),
                                                        rewardDelta -> {
                                                            logger.info(TAG, "Awarded reward '" + reward.getName() + "' for challenge " + challengeId + ". Delta: " + rewardDelta);
                                                            return rewardDelta;
                                                        }, MoreExecutors.directExecutor());
                                            }, ioExecutor);
                                }, ioExecutor),
                        ioExecutor);
            } else {
                logger.debug(TAG, "Challenge " + challengeId + " is not yet fully completed for this period.");
                return Futures.immediateFuture(new ApplyRewardUseCase.RewardApplicationResult(0, 0));
            }
        }, ioExecutor);
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
        // TODO: Добавить обработку TASK_STREAK, POMODORO_STREAK, WATERING_STREAK, CUSTOM_EVENT, etc.
        // if (event instanceof GamificationEvent.Custom && rule.getType() == ChallengeType.CUSTOM_EVENT) { typeMatches = true; }

        if (!typeMatches) return false;
        return checkRuleConditions(rule, event);
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
                // Пример для "minPriority"
                if (conditions.has("minPriority")) {
                    // Логика сравнения приоритетов (потребует доступа к Task или его Priority)
                    // Это усложняет, так как GamificationEvent.TaskCompleted не несет Priority.
                    // Возможно, это условие должно проверяться на более высоком уровне или событие должно нести больше данных.
                }
                // Пример для "maxHour" / "minHour"
                if (conditions.has("maxHour")) {
                    // Потребуется время выполнения задачи из статистики или самого Task.
                }

            } else if (event instanceof GamificationEvent.PomodoroCompleted) {
                GamificationEvent.PomodoroCompleted pomodoroEvent = (GamificationEvent.PomodoroCompleted) event;
                if (conditions.has("minDurationMinutes")) {
                    if (pomodoroEvent.getDurationSeconds() < conditions.getInt("minDurationMinutes") * 60) {
                        return false;
                    }
                }
                if (conditions.has("interruptions")) {
                    // Потребуется информация о прерываниях из PomodoroSession
                    // GamificationEvent.PomodoroCompleted должен будет нести эту информацию.
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
            // TODO: Добавить обработку условий для других типов событий и ChallengeType
            // (CUSTOM_EVENT, BADGE_COUNT, PLANT_MAX_STAGE, LEVEL_REACHED, RESOURCE_ACCUMULATED, STATISTIC_REACHED)
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
        LocalDateTime now = LocalDateTime.now(); // Используем системное время для сравнения
        LocalDateTime lastUpdated = progress.getLastUpdated(); // Это время UTC из БД

        // Для корректного сравнения периодов (день, неделя, месяц)
        // лучше конвертировать lastUpdated в локальную зону устройства,
        // если периоды должны считаться по локальному времени пользователя.
        // Если периоды считаются по UTC, то можно оставить как есть.
        // Предположим, что DateTimeUtils.utcToLocalLocalDateTime доступен.
        // Если нет, то сравнение будет по UTC.
        // DateTimeUtils dateTimeUtils = new DateTimeUtils(); // Плохо создавать здесь, лучше инжектить
        // LocalDateTime localLastUpdated = dateTimeUtils.utcToLocalLocalDateTime(lastUpdated);
        // LocalDate localNowDate = now.toLocalDate();
        // LocalDate localLastUpdateDate = localLastUpdated.toLocalDate();

        // Пока будем сравнивать по UTC для простоты, но это может быть неточно для пользователя
        LocalDate nowDateUtc = now.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
        LocalDate lastUpdateDateUtc = lastUpdated.toLocalDate(); // lastUpdated уже UTC

        switch (rule.getPeriod()) {
            case DAILY:
                return lastUpdateDateUtc.isEqual(nowDateUtc);
            case WEEKLY:
                WeekFields weekFields = WeekFields.of(Locale.getDefault()); // Можно использовать ISO или системную локаль
                return lastUpdated.getYear() == now.getYear() && // Сравниваем год UTC
                        lastUpdated.get(weekFields.weekOfWeekBasedYear()) == now.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).get(weekFields.weekOfWeekBasedYear());
            case MONTHLY:
                return lastUpdated.getYear() == now.getYear() && // Сравниваем год UTC
                        lastUpdated.getMonth() == now.atZone(ZoneOffset.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).getMonth();
            default:
                return true;
        }
    }
}