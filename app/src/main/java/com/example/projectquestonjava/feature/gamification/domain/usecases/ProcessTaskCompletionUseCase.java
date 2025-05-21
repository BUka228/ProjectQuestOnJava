package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.model.GamificationConstants;
import com.example.projectquestonjava.feature.gamification.domain.model.GamificationEvent;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
import com.example.projectquestonjava.feature.statistics.data.model.TaskStatistics;
import com.example.projectquestonjava.feature.statistics.domain.repository.GamificationHistoryRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.GlobalStatisticsRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.TaskStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class ProcessTaskCompletionUseCase {
    private static final String TAG = "ProcessTaskCompletionUC";
    // Константы перенесены из Kotlin объекта GamificationConstants
    private static final String BASE_XP_REWARD_VALUE = GamificationConstants.POMODORO_FOCUS_XP_REWARD_VALUE; // Пример, возможно нужно другое значение
    private static final String BASE_COIN_REWARD_VALUE = GamificationConstants.POMODORO_FOCUS_COIN_REWARD_VALUE; // Пример
    private static final String HISTORY_REASON_TASK_COMPLETED = GamificationConstants.HISTORY_REASON_TASK_COMPLETED;


    private final TaskRepository taskRepository;
    private final UnitOfWork unitOfWork;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final GamificationRepository gamificationRepository;
    private final TaskStatisticsRepository taskStatisticsRepository;
    private final GamificationHistoryRepository gamificationHistoryRepository;
    private final GlobalStatisticsRepository globalStatisticsRepository;
    private final ApplyRewardUseCase applyRewardUseCase;
    private final UpdateChallengeProgressUseCase updateChallengeProgressUseCase;
    private final ApplyGrowthPointsUseCase applyGrowthPointsUseCase;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public ProcessTaskCompletionUseCase(
            TaskRepository taskRepository,
            UnitOfWork unitOfWork,
            GamificationDataStoreManager gamificationDataStoreManager,
            GamificationRepository gamificationRepository,
            TaskStatisticsRepository taskStatisticsRepository,
            GamificationHistoryRepository gamificationHistoryRepository,
            GlobalStatisticsRepository globalStatisticsRepository,
            ApplyRewardUseCase applyRewardUseCase,
            UpdateChallengeProgressUseCase updateChallengeProgressUseCase,
            ApplyGrowthPointsUseCase applyGrowthPointsUseCase,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskRepository = taskRepository;
        this.unitOfWork = unitOfWork;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.gamificationRepository = gamificationRepository;
        this.taskStatisticsRepository = taskStatisticsRepository;
        this.gamificationHistoryRepository = gamificationHistoryRepository;
        this.globalStatisticsRepository = globalStatisticsRepository;
        this.applyRewardUseCase = applyRewardUseCase;
        this.updateChallengeProgressUseCase = updateChallengeProgressUseCase;
        this.applyGrowthPointsUseCase = applyGrowthPointsUseCase;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(long taskId, List<Tag> tags) {
        return Futures.submitAsync(() -> {
            try {
                Long gamificationId = Futures.getDone(gamificationDataStoreManager.getGamificationIdFuture());
                LocalDateTime completionTimeUtc = dateTimeUtils.currentUtcDateTime();

                unitOfWork.withTransaction((Callable<Void>) () -> {
                    logger.debug(TAG, "Processing task completion: taskId=" + taskId + ", gamiId=" + (gamificationId != null ? gamificationId : "N/A"));

                    // Шаг 1: Обновление статуса задачи
                    Futures.getDone(taskRepository.updateTaskStatus(taskId, TaskStatus.DONE));
                    logger.debug(TAG, "Task " + taskId + " status updated to DONE.");

                    // Шаг 2: Статистика и проверка первого завершения
                    TaskStatistics defaultStats = new TaskStatistics(taskId, null, 0, 0, 0, 0, false);
                    TaskStatistics statistics = Futures.getDone(taskStatisticsRepository.ensureAndGetStatistics(taskId, defaultStats));
                    boolean wasCompletedOnce = statistics.isWasCompletedOnce();
                    logger.debug(TAG, "Task " + taskId + " statistics ensured/retrieved: wasCompletedOnce = " + wasCompletedOnce);

                    int finalDeltaXp = 0;
                    int finalDeltaCoins = 0;
                    Gamification gamificationToUpdate = null;

                    // Шаг 3: Геймификация (Только при первом завершении)
                    if (!wasCompletedOnce) {
                        Futures.getDone(globalStatisticsRepository.incrementCompletedTasks());
                        logger.debug(TAG, "Incremented global completed tasks.");

                        if (gamificationId != null && gamificationId != -1L) {
                            logger.info(TAG, "First completion of task " + taskId + ". Processing gamification...");
                            gamificationToUpdate = Futures.getDone(gamificationRepository.getGamificationById(gamificationId));
                            if (gamificationToUpdate == null) {
                                throw new IllegalStateException("Gamification data not found for ID " + gamificationId);
                            }

                            Reward baseXpReward = new Reward("XP за задачу", "XP за задачу", RewardType.EXPERIENCE, BASE_XP_REWARD_VALUE);
                            Reward baseCoinReward = new Reward("Монеты за задачу", "Монеты за задачу", RewardType.COINS, BASE_COIN_REWARD_VALUE);

                            ApplyRewardUseCase.RewardApplicationResult xpResult = Futures.getDone(applyRewardUseCase.execute(gamificationId, baseXpReward));
                            finalDeltaXp += xpResult.getDeltaXp();
                            ApplyRewardUseCase.RewardApplicationResult coinResult = Futures.getDone(applyRewardUseCase.execute(gamificationId, baseCoinReward));
                            finalDeltaCoins += coinResult.getDeltaCoins();
                            logger.debug(TAG, "Applied base rewards. Current Delta(XP/Coins): (" + finalDeltaXp + "/" + finalDeltaCoins + ")");

                            GamificationEvent event = new GamificationEvent.TaskCompleted(taskId, tags);
                            ApplyRewardUseCase.RewardApplicationResult challengeResult = Futures.getDone(updateChallengeProgressUseCase.execute(gamificationId, event));
                            finalDeltaXp += challengeResult.getDeltaXp();
                            finalDeltaCoins += challengeResult.getDeltaCoins();
                            logger.debug(TAG, "Processed challenge progress. Challenge Delta(XP/Coins): (" + challengeResult.getDeltaXp() + "/" + challengeResult.getDeltaCoins() + "). Total Delta: (" + finalDeltaXp + "/" + finalDeltaCoins + ")");

                            Futures.getDone(taskStatisticsRepository.markTaskAsCompletedOnce(taskId));
                            logger.info(TAG, "Task " + taskId + " marked as completed once.");

                            Long selectedPlantId = Futures.getDone(gamificationDataStoreManager.getSelectedPlantIdFuture());
                            if (selectedPlantId != null && selectedPlantId != -1L) {
                                int pointsPerTask = 1; // Можно вынести в GamificationConstants
                                try {
                                    Futures.getDone(applyGrowthPointsUseCase.execute(selectedPlantId, pointsPerTask));
                                    logger.debug(TAG, "Applied " + pointsPerTask + " GP for task " + taskId + " completion to plant " + selectedPlantId + ".");
                                } catch (Exception growthError) {
                                    logger.error(TAG, "Non-critical: Failed to apply GP for task completion to plant " + selectedPlantId, growthError);
                                }
                            }
                        } else {
                            logger.warn(TAG, "Gamification ID not found for first completion of task " + taskId + ". Skipping gamification effects.");
                            Futures.getDone(taskStatisticsRepository.markTaskAsCompletedOnce(taskId));
                            logger.info(TAG, "Task " + taskId + " marked as completed once (no gamification profile).");
                        }
                    } else {
                        logger.info(TAG, "Task " + taskId + " was already completed once. Skipping gamification rewards/challenges/growth.");
                    }

                    // Шаг 4: Обновление статистики задачи
                    Futures.getDone(taskStatisticsRepository.updateCompletionTime(taskId, completionTimeUtc));
                    logger.debug(TAG, "Updated completionTime in statistics for task " + taskId + ".");

                    // Шаг 5: Сохранение итогового состояния геймификации
                    if (gamificationToUpdate != null) {
                        Gamification finalGamification = new Gamification(
                                gamificationToUpdate.getId(), gamificationToUpdate.getUserId(),
                                gamificationToUpdate.getLevel(), // Пересчет уровня в репозитории
                                Math.max(0, gamificationToUpdate.getExperience() + finalDeltaXp),
                                Math.max(0, gamificationToUpdate.getCoins() + finalDeltaCoins),
                                gamificationToUpdate.getMaxExperienceForLevel(), // Пересчет в репозитории
                                completionTimeUtc, // lastActive
                                gamificationToUpdate.getCurrentStreak(),
                                gamificationToUpdate.getLastClaimedDate(),
                                gamificationToUpdate.getMaxStreak()
                        );
                        Futures.getDone(gamificationRepository.updateGamification(finalGamification));
                        logger.info(TAG, "Final Gamification state saved for user " + gamificationId + ". Applied Delta(XP/Coins): (" + finalDeltaXp + "/" + finalDeltaCoins + ")");

                        Futures.getDone(globalStatisticsRepository.updateLastActive());
                        logger.debug(TAG, "Updated global last active time.");

                        // Шаг 6: Запись в ИСТОРИЮ геймификации
                        if (finalDeltaXp != 0 || finalDeltaCoins != 0) {
                            GamificationHistory historyEntry = new GamificationHistory(
                                    0, gamificationId, completionTimeUtc,
                                    finalDeltaXp, finalDeltaCoins,
                                    HISTORY_REASON_TASK_COMPLETED, taskId
                            );
                            try {
                                Futures.getDone(gamificationHistoryRepository.insertHistoryEntry(historyEntry));
                                logger.debug(TAG, "Gamification history entry created for task " + taskId + " completion.");
                            } catch (Exception historyError) {
                                logger.error(TAG, "Failed to insert Pomodoro gamification history", historyError);
                            }
                        } else {
                            logger.debug(TAG, "No XP/Coin changes, skipping history entry.");
                        }
                    } else {
                        logger.debug(TAG, "No gamification profile to update or no significant changes occurred.");
                    }
                    logger.info(TAG, "Task " + taskId + " completion processed successfully within transaction.");
                    return null; // Для Callable<Void>
                });
                return Futures.immediateFuture(null); // Успешное завершение
            } catch (Exception e) {
                logger.error(TAG, "Failed to process task completion for taskId=" + taskId, e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }
}