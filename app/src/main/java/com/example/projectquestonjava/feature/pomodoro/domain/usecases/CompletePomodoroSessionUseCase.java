package com.example.projectquestonjava.feature.pomodoro.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork; // Если используется для транзакций
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.model.GamificationConstants;
import com.example.projectquestonjava.feature.gamification.domain.model.GamificationEvent;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.gamification.domain.usecases.ApplyGrowthPointsUseCase;
import com.example.projectquestonjava.feature.gamification.domain.usecases.ApplyRewardUseCase;
import com.example.projectquestonjava.feature.gamification.domain.usecases.UpdateChallengeProgressUseCase;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSessionRepository;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
import com.example.projectquestonjava.feature.statistics.domain.repository.GamificationHistoryRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.GlobalStatisticsRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.TaskStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class CompletePomodoroSessionUseCase {
    private static final String TAG = "CompletePomodoroSessionUC";

    private final PomodoroSessionRepository pomodoroSessionRepository;
    private final TaskStatisticsRepository taskStatisticsRepository;
    private final GamificationRepository gamificationRepository;
    private final GamificationHistoryRepository gamificationHistoryRepository;
    private final GlobalStatisticsRepository globalStatisticsRepository;
    private final ApplyRewardUseCase applyRewardUseCase;
    private final UpdateChallengeProgressUseCase updateChallengeProgressUseCase;
    private final ApplyGrowthPointsUseCase applyGrowthPointsUseCase;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;
    private final UnitOfWork unitOfWork; // Добавим UnitOfWork для транзакций

    @Inject
    public CompletePomodoroSessionUseCase(
            PomodoroSessionRepository pomodoroSessionRepository,
            TaskStatisticsRepository taskStatisticsRepository,
            GamificationRepository gamificationRepository,
            GamificationHistoryRepository gamificationHistoryRepository,
            GlobalStatisticsRepository globalStatisticsRepository,
            ApplyRewardUseCase applyRewardUseCase,
            UpdateChallengeProgressUseCase updateChallengeProgressUseCase,
            ApplyGrowthPointsUseCase applyGrowthPointsUseCase,
            GamificationDataStoreManager gamificationDataStoreManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger,
            UnitOfWork unitOfWork) {
        this.pomodoroSessionRepository = pomodoroSessionRepository;
        this.taskStatisticsRepository = taskStatisticsRepository;
        this.gamificationRepository = gamificationRepository;
        this.gamificationHistoryRepository = gamificationHistoryRepository;
        this.globalStatisticsRepository = globalStatisticsRepository;
        this.applyRewardUseCase = applyRewardUseCase;
        this.updateChallengeProgressUseCase = updateChallengeProgressUseCase;
        this.applyGrowthPointsUseCase = applyGrowthPointsUseCase;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.unitOfWork = unitOfWork;
    }

    public ListenableFuture<Void> execute(
            long sessionId,
            long taskId,
            SessionType type,
            int actualDurationSeconds,
            int interruptionsInPhase
    ) {
        return Futures.submitAsync(() -> { // Выполняем всю логику на ioExecutor
            try {
                // Получаем gamificationId асинхронно (если он нужен до транзакции)
                // или синхронно внутри транзакции, если DataStoreManager это позволяет безопасно
                Long gamificationId = Futures.getDone(gamificationDataStoreManager.getGamificationIdFuture());
                LocalDateTime completionTimeUtc = dateTimeUtils.currentUtcDateTime();

                logger.debug(TAG, "Processing completion for PomodoroSession ID " + sessionId +
                        " (Task " + taskId + ", Type " + type +
                        ", ActualDur " + actualDurationSeconds + "s, Interrupts " + interruptionsInPhase + ")");

                // Обернем все операции с БД в одну транзакцию
                unitOfWork.withTransaction((Callable<Void>) () -> {
                    // 1. Обновить PomodoroSession
                    PomodoroSession sessionEntry = Futures.getDone(pomodoroSessionRepository.getSessionById(sessionId));
                    if (sessionEntry == null) {
                        throw new IllegalStateException("PomodoroSession with ID " + sessionId + " not found for update.");
                    }
                    // Создаем новый объект для обновления, т.к. PomodoroSession может быть immutable
                    PomodoroSession updatedSessionEntry = new PomodoroSession(
                            sessionEntry.getId(), sessionEntry.getUserId(), sessionEntry.getTaskId(),
                            sessionEntry.getStartTime(), sessionEntry.getSessionType(),
                            sessionEntry.getPlannedDurationSeconds(), actualDurationSeconds,
                            interruptionsInPhase, true // completed = true
                    );
                    Futures.getDone(pomodoroSessionRepository.updateSession(updatedSessionEntry));
                    logger.debug(TAG, "PomodoroSession " + sessionId + " updated in DB.");

                    // 2. Обновить TaskStatistics
                    int minFocusDurationSecForStatsAndReward = GamificationConstants.MIN_FOCUS_DURATION_FOR_REWARD_MINUTES * 60;

                    if (type.isFocus()) {
                        Futures.getDone(taskStatisticsRepository.addTimeToSpent(taskId, actualDurationSeconds));
                        Futures.getDone(taskStatisticsRepository.addTotalPomodoroFocusTime(taskId, actualDurationSeconds));
                        if (actualDurationSeconds >= minFocusDurationSecForStatsAndReward) {
                            Futures.getDone(taskStatisticsRepository.incrementCompletedPomodoroFocusSessions(taskId));
                        }
                    }
                    Futures.getDone(taskStatisticsRepository.incrementTotalPomodoroInterruptions(taskId, interruptionsInPhase));
                    logger.debug(TAG, "TaskStatistics for " + taskId + " updated.");

                    // 3. Геймификация (только для значимых FOCUS сессий)
                    if (type.isFocus() && actualDurationSeconds >= minFocusDurationSecForStatsAndReward && gamificationId != null && gamificationId != -1L) {
                        logger.debug(TAG, "Processing gamification for FOCUS session " + sessionId + " completion.");
                        Gamification currentGamification = Futures.getDone(gamificationRepository.getGamificationById(gamificationId));
                        if (currentGamification == null) {
                            throw new IllegalStateException("Gamification data not found for ID " + gamificationId);
                        }

                        int accumulatedDeltaXp = 0;
                        int accumulatedDeltaCoins = 0;

                        Reward pomodoroXpReward = new Reward(0,"XP за Pomodoro", "XP за фокус-сессию", RewardType.EXPERIENCE, GamificationConstants.POMODORO_FOCUS_XP_REWARD_VALUE);
                        Reward pomodoroCoinReward = new Reward(0,"Монеты за Pomodoro", "Монеты за фокус-сессию", RewardType.COINS, GamificationConstants.POMODORO_FOCUS_COIN_REWARD_VALUE);

                        ApplyRewardUseCase.RewardApplicationResult xpResult = Futures.getDone(applyRewardUseCase.execute(gamificationId, pomodoroXpReward));
                        accumulatedDeltaXp += xpResult.getDeltaXp();
                        ApplyRewardUseCase.RewardApplicationResult coinResult = Futures.getDone(applyRewardUseCase.execute(gamificationId, pomodoroCoinReward));
                        accumulatedDeltaCoins += coinResult.getDeltaCoins();
                        logger.debug(TAG, "Base Pomodoro rewards applied. Delta(XP/Coins): (" + accumulatedDeltaXp + "/" + accumulatedDeltaCoins + ")");

                        GamificationEvent event = new GamificationEvent.PomodoroCompleted(sessionId, actualDurationSeconds, taskId);
                        ApplyRewardUseCase.RewardApplicationResult challengeResult = Futures.getDone(updateChallengeProgressUseCase.execute(gamificationId, event));
                        accumulatedDeltaXp += challengeResult.getDeltaXp();
                        accumulatedDeltaCoins += challengeResult.getDeltaCoins();
                        logger.debug(TAG, "Challenge progress updated. Total Delta(XP/Coins): (" + accumulatedDeltaXp + "/" + accumulatedDeltaCoins + ")");

                        Long selectedPlantId = Futures.getDone(gamificationDataStoreManager.getSelectedPlantIdFuture());
                        if (selectedPlantId != null && selectedPlantId != -1L) {
                            try {
                                Futures.getDone(applyGrowthPointsUseCase.execute(selectedPlantId, GamificationConstants.GROWTH_POINTS_PER_COMPLETED_FOCUS_SESSION));
                                logger.debug(TAG, "Applied " + GamificationConstants.GROWTH_POINTS_PER_COMPLETED_FOCUS_SESSION + " GP to plant " + selectedPlantId);
                            } catch (Exception growthError) {
                                logger.error(TAG, "Non-critical: Failed to apply GP for Pomodoro to plant " + selectedPlantId, growthError);
                            }
                        }

                        Gamification gamificationToUpdate = new Gamification(
                                currentGamification.getId(), currentGamification.getUserId(),
                                currentGamification.getLevel(), // Пересчет в репо
                                Math.max(0, currentGamification.getExperience() + accumulatedDeltaXp),
                                Math.max(0, currentGamification.getCoins() + accumulatedDeltaCoins),
                                currentGamification.getMaxExperienceForLevel(), // Пересчет в репо
                                completionTimeUtc, // lastActive
                                currentGamification.getCurrentStreak(), // Стрик обновляется ClaimDailyRewardUseCase
                                currentGamification.getLastClaimedDate(),
                                currentGamification.getMaxStreak()
                        );
                        Futures.getDone(gamificationRepository.updateGamification(gamificationToUpdate));

                        if (accumulatedDeltaXp != 0 || accumulatedDeltaCoins != 0) {
                            GamificationHistory historyEntry = new GamificationHistory(
                                    0, gamificationId, completionTimeUtc,
                                    accumulatedDeltaXp, accumulatedDeltaCoins,
                                    GamificationConstants.HISTORY_REASON_POMODORO_COMPLETED, taskId
                            );
                            try {
                                Futures.getDone(gamificationHistoryRepository.insertHistoryEntry(historyEntry));
                            } catch (Exception histError) {
                                logger.error(TAG, "Failed to insert Pomodoro gamification history", histError);
                            }
                        }
                        logger.info(TAG, "Gamification profile updated for FOCUS session " + sessionId);
                    } else if (type.isFocus() && (gamificationId != null && gamificationId != -1L)) {
                        Gamification currentGamification = Futures.getDone(gamificationRepository.getGamificationById(gamificationId));
                        if (currentGamification != null) {
                            Gamification profileToUpdate = new Gamification( // Создаем новый объект, чтобы избежать изменения оригинала
                                    currentGamification.getId(), currentGamification.getUserId(),
                                    currentGamification.getLevel(), currentGamification.getExperience(),
                                    currentGamification.getCoins(), currentGamification.getMaxExperienceForLevel(),
                                    completionTimeUtc, // Обновляем lastActive
                                    currentGamification.getCurrentStreak(), currentGamification.getLastClaimedDate(),
                                    currentGamification.getMaxStreak()
                            );
                            try {
                                Futures.getDone(gamificationRepository.updateGamification(profileToUpdate));
                            } catch (Exception e){
                                logger.error(TAG, "Failed to update lastActive after short focus", e);
                            }
                        }
                    }

                    // 4. Обновить GlobalStatistics
                    if (type.isFocus() && actualDurationSeconds >= minFocusDurationSecForStatsAndReward) {
                        Futures.getDone(globalStatisticsRepository.addTotalTimeSpent(actualDurationSeconds / 60));
                    }
                    Futures.getDone(globalStatisticsRepository.updateLastActive());
                    logger.debug(TAG, "GlobalStatistics updated.");
                    logger.info(TAG, "PomodoroSession ID " + sessionId + " (Task " + taskId + ", Type " + type + ") completion processing finished successfully.");
                    return null; // Для Callable<Void>
                });
                return Futures.immediateFuture(null); // Успех
            } catch (Exception e) {
                logger.error(TAG, "Error completing Pomodoro session " + sessionId, e);
                return Futures.immediateFailedFuture(e); // Провал
            }
        }, ioExecutor);
    }
}