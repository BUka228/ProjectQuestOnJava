package com.example.projectquestonjava.feature.pomodoro.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.UserSessionManager; // Импорт
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

import java.time.LocalDateTime;
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
    private final UserSessionManager userSessionManager; // Добавлен
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;
    private final UnitOfWork unitOfWork;

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
            UserSessionManager userSessionManager, // Внедряем
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
        this.userSessionManager = userSessionManager; // Сохраняем
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.unitOfWork = unitOfWork;
    }

    public ListenableFuture<Void> execute(
            long sessionId, long taskId, SessionType type,
            int actualDurationSeconds, int interruptionsInPhase) {
        return Futures.submit(() -> {
            try {
                // userId и gamificationId получаем синхронно, т.к. мы на ioExecutor
                int userId = userSessionManager.getUserIdSync();
                if (userId == UserSessionManager.NO_USER_ID) {
                    throw new IllegalStateException("User not logged in for CompletePomodoroSession.");
                }
                Long gamificationId = gamificationDataStoreManager.getGamificationIdSync();
                LocalDateTime completionTimeUtc = dateTimeUtils.currentUtcDateTime();

                logger.debug(TAG, "Processing completion for PomodoroSession ID " + sessionId +
                        " (Task " + taskId + ", Type " + type + ", User " + userId +
                        ", ActualDur " + actualDurationSeconds + "s, Interrupts " + interruptionsInPhase + ")");

                unitOfWork.withTransaction((Callable<Void>) () -> {
                    PomodoroSession sessionEntry = pomodoroSessionRepository.getSessionByIdSync(sessionId);
                    if (sessionEntry == null) {
                        throw new IllegalStateException("PomodoroSession ID " + sessionId + " not found.");
                    }
                    if (sessionEntry.getUserId() != userId) { // Проверка принадлежности сессии пользователю
                        throw new SecurityException("Attempt to complete session of another user.");
                    }

                    PomodoroSession updatedSessionEntry = new PomodoroSession(
                            sessionEntry.getId(), sessionEntry.getUserId(), sessionEntry.getTaskId(),
                            sessionEntry.getStartTime(), sessionEntry.getSessionType(),
                            sessionEntry.getPlannedDurationSeconds(), actualDurationSeconds,
                            interruptionsInPhase, true
                    );
                    pomodoroSessionRepository.updateSessionSync(updatedSessionEntry);
                    logger.debug(TAG, "PomodoroSession " + sessionId + " updated in DB.");

                    int minFocusDurationSecForStatsAndReward = GamificationConstants.MIN_FOCUS_DURATION_FOR_REWARD_MINUTES * 60;
                    if (type.isFocus()) {
                        taskStatisticsRepository.addTimeToSpentSync(taskId, actualDurationSeconds);
                        taskStatisticsRepository.addTotalPomodoroFocusTimeSync(taskId, actualDurationSeconds);
                        if (actualDurationSeconds >= minFocusDurationSecForStatsAndReward) {
                            taskStatisticsRepository.incrementCompletedPomodoroFocusSessionsSync(taskId);
                        }
                    }
                    taskStatisticsRepository.incrementTotalPomodoroInterruptionsSync(taskId, interruptionsInPhase);
                    logger.debug(TAG, "TaskStatistics for " + taskId + " updated.");

                    if (type.isFocus() && actualDurationSeconds >= minFocusDurationSecForStatsAndReward && gamificationId != -1L) {
                        Gamification currentGami = gamificationRepository.getGamificationByIdSync(gamificationId);
                        if (currentGami == null) throw new IllegalStateException("Gamification not found for ID " + gamificationId);

                        ApplyRewardUseCase.RewardApplicationResult xpResult = applyRewardUseCase.execute(gamificationId,
                                new Reward("XP за Pomodoro", "", RewardType.EXPERIENCE, GamificationConstants.POMODORO_FOCUS_XP_REWARD_VALUE));
                        ApplyRewardUseCase.RewardApplicationResult coinResult = applyRewardUseCase.execute(gamificationId,
                                new Reward("Монеты за Pomodoro", "", RewardType.COINS, GamificationConstants.POMODORO_FOCUS_COIN_REWARD_VALUE));
                        int deltaXp = xpResult.getDeltaXp() + coinResult.getDeltaXp();
                        int deltaCoins = coinResult.getDeltaCoins() + coinResult.getDeltaCoins(); // ОШИБКА БЫЛА ЗДЕСЬ: baseRewards -> coinRewards

                        GamificationEvent event = new GamificationEvent.PomodoroCompleted(sessionId, actualDurationSeconds, taskId);
                        ApplyRewardUseCase.RewardApplicationResult challengeDelta = updateChallengeProgressUseCase.executeSync(gamificationId, event);
                        deltaXp += challengeDelta.getDeltaXp();
                        deltaCoins += challengeDelta.getDeltaCoins();

                        long selectedPlantId = gamificationDataStoreManager.getSelectedPlantIdSync();
                        if (selectedPlantId != -1L) {
                            applyGrowthPointsUseCase.execute(selectedPlantId, GamificationConstants.GROWTH_POINTS_PER_COMPLETED_FOCUS_SESSION).get();
                        }

                        Gamification gamificationToUpdate = new Gamification(
                                currentGami.getId(), currentGami.getUserId(), currentGami.getLevel(),
                                Math.max(0, currentGami.getExperience() + deltaXp),
                                Math.max(0, currentGami.getCoins() + deltaCoins),
                                currentGami.getMaxExperienceForLevel(), completionTimeUtc,
                                currentGami.getCurrentStreak(), currentGami.getLastClaimedDate(),
                                currentGami.getMaxStreak()
                        );
                        gamificationRepository.updateGamificationSync(gamificationToUpdate);

                        if (deltaXp != 0 || deltaCoins != 0) {
                            gamificationHistoryRepository.insertHistoryEntrySync(new GamificationHistory(
                                    gamificationId, completionTimeUtc, deltaXp, deltaCoins,
                                    GamificationConstants.HISTORY_REASON_POMODORO_COMPLETED, taskId
                            ));
                        }
                        logger.info(TAG, "Gamification updated for FOCUS session " + sessionId);
                    } else if (type.isFocus() && gamificationId != -1L) {
                        Gamification currentGami = gamificationRepository.getGamificationByIdSync(gamificationId);
                        if (currentGami != null) {
                            currentGami.setLastActive(completionTimeUtc); // Только обновляем lastActive
                            gamificationRepository.updateGamificationSync(currentGami);
                        }
                    }

                    if (type.isFocus() && actualDurationSeconds >= minFocusDurationSecForStatsAndReward) {
                        globalStatisticsRepository.addTotalTimeSpentSync(actualDurationSeconds / 60);
                    }
                    globalStatisticsRepository.updateLastActiveSync();
                    logger.debug(TAG, "GlobalStatistics updated.");
                    return null;
                });
                return null;
            } catch (Exception e) {
                logger.error(TAG, "Error completing Pomodoro session " + sessionId, e);
                throw e;
            }
        }, ioExecutor);
    }
}