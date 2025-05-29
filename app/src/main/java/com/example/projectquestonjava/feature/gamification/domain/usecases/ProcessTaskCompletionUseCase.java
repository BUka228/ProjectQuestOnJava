package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class ProcessTaskCompletionUseCase {
    private static final String TAG = "ProcessTaskCompletionUC";
    private static final String BASE_XP_REWARD_VALUE = GamificationConstants.POMODORO_FOCUS_XP_REWARD_VALUE;
    private static final String BASE_COIN_REWARD_VALUE = GamificationConstants.POMODORO_FOCUS_COIN_REWARD_VALUE;
    private static final String HISTORY_REASON_TASK_COMPLETED = GamificationConstants.HISTORY_REASON_TASK_COMPLETED;

    private final TaskRepository taskRepository;
    private final UnitOfWork unitOfWork;
    private final UserSessionManager userSessionManager;
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
            TaskRepository taskRepository, UnitOfWork unitOfWork,
            UserSessionManager userSessionManager,
            GamificationDataStoreManager gamificationDataStoreManager, GamificationRepository gamificationRepository,
            TaskStatisticsRepository taskStatisticsRepository, GamificationHistoryRepository gamificationHistoryRepository,
            GlobalStatisticsRepository globalStatisticsRepository, ApplyRewardUseCase applyRewardUseCase,
            UpdateChallengeProgressUseCase updateChallengeProgressUseCase, ApplyGrowthPointsUseCase applyGrowthPointsUseCase,
            DateTimeUtils dateTimeUtils, @IODispatcher Executor ioExecutor, Logger logger) {
        this.taskRepository = taskRepository;
        this.unitOfWork = unitOfWork;
        this.userSessionManager = userSessionManager;
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
        return Futures.submit(() -> {
            logger.info(TAG, "execute: START for taskId=" + taskId); // <-- ЛОГ НАЧАЛА
            try {
                int userId = userSessionManager.getUserIdSync();
                if (userId == UserSessionManager.NO_USER_ID) {
                    logger.error(TAG, "execute: User not logged in.");
                    throw new IllegalStateException("User not logged in for task completion processing.");
                }

                Long gamificationId = gamificationDataStoreManager.getGamificationIdSync();
                LocalDateTime completionTimeUtc = dateTimeUtils.currentUtcDateTime();

                unitOfWork.withTransaction((Callable<Void>) () -> {
                    logger.debug(TAG, "execute: Transaction START for taskId=" + taskId);

                    taskRepository.updateTaskStatusSync(taskId, userId, TaskStatus.DONE, completionTimeUtc);
                    logger.debug(TAG, "execute: Task " + taskId + " status updated to DONE (SYNC).");

                    TaskStatistics defaultStats = new TaskStatistics(taskId, null, 0, 0, 0, 0, false);
                    TaskStatistics statistics = taskStatisticsRepository.ensureAndGetStatisticsSync(taskId, defaultStats);
                    boolean wasCompletedOnce = statistics.isWasCompletedOnce();
                    logger.debug(TAG, "execute: Task wasCompletedOnce: " + wasCompletedOnce);


                    int finalDeltaXp = 0;
                    int finalDeltaCoins = 0;
                    Gamification gamificationToUpdate = null;

                    if (!wasCompletedOnce) {
                        logger.debug(TAG, "execute: Processing first completion rewards for taskId=" + taskId);
                        globalStatisticsRepository.incrementCompletedTasksSync();
                        logger.debug(TAG, "execute: Incremented global completed tasks.");

                        if (gamificationId != null && gamificationId != -1L) {
                            logger.debug(TAG, "execute: Gamification ID " + gamificationId + " is valid.");
                            gamificationToUpdate = gamificationRepository.getGamificationByIdSync(gamificationId);
                            if (gamificationToUpdate == null) {
                                logger.error(TAG, "execute: Gamification data not found for ID " + gamificationId);
                                throw new IllegalStateException("Gamification data not found for ID " + gamificationId);
                            }
                            logger.debug(TAG, "execute: Gamification profile loaded for ID " + gamificationId);


                            logger.debug(TAG, "execute: Applying base XP reward...");
                            ApplyRewardUseCase.RewardApplicationResult xpResult = applyRewardUseCase.execute(gamificationId, new Reward("XP за задачу", "", RewardType.EXPERIENCE, BASE_XP_REWARD_VALUE));
                            finalDeltaXp += xpResult.getDeltaXp();
                            logger.debug(TAG, "execute: Base XP applied. Delta: " + xpResult.getDeltaXp());

                            logger.debug(TAG, "execute: Applying base Coin reward...");
                            ApplyRewardUseCase.RewardApplicationResult coinResult = applyRewardUseCase.execute(gamificationId, new Reward("Монеты за задачу", "", RewardType.COINS, BASE_COIN_REWARD_VALUE));
                            finalDeltaCoins += coinResult.getDeltaCoins();
                            logger.debug(TAG, "execute: Base Coins applied. Delta: " + coinResult.getDeltaCoins());


                            logger.debug(TAG, "execute: Updating challenge progress...");
                            GamificationEvent event = new GamificationEvent.TaskCompleted(taskId, tags);
                            ApplyRewardUseCase.RewardApplicationResult challengeResult = updateChallengeProgressUseCase.executeSync(gamificationId, event);
                            finalDeltaXp += challengeResult.getDeltaXp();
                            finalDeltaCoins += challengeResult.getDeltaCoins();
                            logger.debug(TAG, "execute: Challenge progress updated. DeltaXP: " + challengeResult.getDeltaXp() + ", DeltaCoins: " + challengeResult.getDeltaCoins());


                            taskStatisticsRepository.markTaskAsCompletedOnceSync(taskId);
                            logger.debug(TAG, "execute: Task " + taskId + " marked as completed once.");

                            long selectedPlantId = gamificationDataStoreManager.getSelectedPlantIdSync();
                            logger.debug(TAG, "execute: Selected plant ID: " + selectedPlantId);
                            if (selectedPlantId != -1L) {
                                logger.debug(TAG, "execute: Applying growth points to plant " + selectedPlantId);
                                applyGrowthPointsUseCase.executeSync(selectedPlantId, GamificationConstants.GROWTH_POINTS_PER_COMPLETED_FOCUS_SESSION);
                                logger.debug(TAG, "execute: Growth points applied to plant " + selectedPlantId);
                            }
                        } else {
                            logger.warn(TAG, "execute: Gamification ID is null or -1. Skipping gamification rewards for first completion.");
                            taskStatisticsRepository.markTaskAsCompletedOnceSync(taskId); // Все равно отмечаем как выполненную
                        }
                    } else {
                        logger.debug(TAG, "execute: Task " + taskId + " was already completed once. Skipping first completion rewards.");
                    }

                    taskStatisticsRepository.updateCompletionTimeSync(taskId, completionTimeUtc);
                    logger.debug(TAG, "execute: Task " + taskId + " completion time updated.");


                    if (gamificationToUpdate != null) { // Блок обновления профиля геймификации
                        logger.debug(TAG, "execute: Preparing to update gamification profile for ID " + gamificationToUpdate.getId());
                        Gamification finalGamification = new Gamification(
                                gamificationToUpdate.getId(), gamificationToUpdate.getUserId(),
                                gamificationToUpdate.getLevel(), Math.max(0, gamificationToUpdate.getExperience() + finalDeltaXp),
                                Math.max(0, gamificationToUpdate.getCoins() + finalDeltaCoins),
                                gamificationToUpdate.getMaxExperienceForLevel(), completionTimeUtc,
                                gamificationToUpdate.getCurrentStreak(), gamificationToUpdate.getLastClaimedDate(),
                                gamificationToUpdate.getMaxStreak()
                        );
                        gamificationRepository.updateGamificationSync(finalGamification);
                        logger.debug(TAG, "execute: Gamification profile updated. New XP: " + finalGamification.getExperience() + ", New Coins: " + finalGamification.getCoins());

                        globalStatisticsRepository.updateLastActiveSync();
                        logger.debug(TAG, "execute: Global statistics last active updated.");

                        if (finalDeltaXp != 0 || finalDeltaCoins != 0) {
                            gamificationHistoryRepository.insertHistoryEntrySync(new GamificationHistory(
                                    gamificationId, completionTimeUtc, finalDeltaXp, finalDeltaCoins,
                                    HISTORY_REASON_TASK_COMPLETED, taskId
                            ));
                            logger.debug(TAG, "execute: Gamification history entry inserted.");
                        }
                    }
                    logger.info(TAG, "execute: Transaction for task " + taskId + " completion finished successfully.");
                    return null;
                });
                logger.info(TAG, "ProcessTaskCompletionUseCase.execute: END for taskId=" + taskId); // <-- ЛОГ ЗАВЕРШЕНИЯ
                return null;
            } catch (Exception e) {
                logger.error(TAG, "ProcessTaskCompletionUseCase.execute: FAILED for taskId=" + taskId, e);
                throw e;
            }
        }, ioExecutor);
    }
}