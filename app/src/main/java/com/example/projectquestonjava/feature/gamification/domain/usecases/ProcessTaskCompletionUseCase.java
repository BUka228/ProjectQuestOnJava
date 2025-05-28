package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager; // <--- ИМПОРТ
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
    private final UserSessionManager userSessionManager; // <--- ДОБАВЛЕНО ПОЛЕ
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
            UserSessionManager userSessionManager, // <--- ВНЕДРЯЕМ UserSessionManager
            GamificationDataStoreManager gamificationDataStoreManager, GamificationRepository gamificationRepository,
            TaskStatisticsRepository taskStatisticsRepository, GamificationHistoryRepository gamificationHistoryRepository,
            GlobalStatisticsRepository globalStatisticsRepository, ApplyRewardUseCase applyRewardUseCase,
            UpdateChallengeProgressUseCase updateChallengeProgressUseCase, ApplyGrowthPointsUseCase applyGrowthPointsUseCase,
            DateTimeUtils dateTimeUtils, @IODispatcher Executor ioExecutor, Logger logger) {
        this.taskRepository = taskRepository;
        this.unitOfWork = unitOfWork;
        this.userSessionManager = userSessionManager; // <--- СОХРАНЯЕМ
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
            try {
                // Получаем userId один раз в начале
                int userId = userSessionManager.getUserIdSync();
                if (userId == UserSessionManager.NO_USER_ID) {
                    logger.error(TAG, "Cannot process task completion: User not logged in.");
                    throw new IllegalStateException("User not logged in for task completion processing.");
                }

                Long gamificationId = gamificationDataStoreManager.getGamificationIdSync();
                LocalDateTime completionTimeUtc = dateTimeUtils.currentUtcDateTime();

                unitOfWork.withTransaction((Callable<Void>) () -> {
                    logger.debug(TAG, "Processing task completion (SYNC): taskId=" + taskId + ", userId=" + userId + ", gamiId=" + (gamificationId != null ? gamificationId : "N/A"));

                    // --- ИСПОЛЬЗУЕМ userId И completionTimeUtc ---
                    taskRepository.updateTaskStatusSync(taskId, userId, TaskStatus.DONE, completionTimeUtc);
                    logger.debug(TAG, "Task " + taskId + " status updated to DONE (SYNC).");

                    TaskStatistics defaultStats = new TaskStatistics(taskId, null, 0, 0, 0, 0, false);
                    TaskStatistics statistics = taskStatisticsRepository.ensureAndGetStatisticsSync(taskId, defaultStats);
                    boolean wasCompletedOnce = statistics.isWasCompletedOnce();

                    int finalDeltaXp = 0;
                    int finalDeltaCoins = 0;
                    Gamification gamificationToUpdate = null;

                    if (!wasCompletedOnce) {
                        globalStatisticsRepository.incrementCompletedTasksSync(); // userId уже используется внутри этого метода репозитория
                        if (gamificationId != -1L) {
                            gamificationToUpdate = gamificationRepository.getGamificationByIdSync(gamificationId);
                            if (gamificationToUpdate == null) throw new IllegalStateException("Gamification data not found for ID " + gamificationId);

                            ApplyRewardUseCase.RewardApplicationResult xpResult = applyRewardUseCase.execute(gamificationId, new Reward("XP за задачу", "", RewardType.EXPERIENCE, BASE_XP_REWARD_VALUE));
                            finalDeltaXp += xpResult.getDeltaXp();
                            ApplyRewardUseCase.RewardApplicationResult coinResult = applyRewardUseCase.execute(gamificationId, new Reward("Монеты за задачу", "", RewardType.COINS, BASE_COIN_REWARD_VALUE));
                            finalDeltaCoins += coinResult.getDeltaCoins();

                            GamificationEvent event = new GamificationEvent.TaskCompleted(taskId, tags);
                            ApplyRewardUseCase.RewardApplicationResult challengeResult = updateChallengeProgressUseCase.executeSync(gamificationId, event);
                            finalDeltaXp += challengeResult.getDeltaXp();
                            finalDeltaCoins += challengeResult.getDeltaCoins();

                            taskStatisticsRepository.markTaskAsCompletedOnceSync(taskId);

                            long selectedPlantId = gamificationDataStoreManager.getSelectedPlantIdSync();
                            if (selectedPlantId != -1L) {
                                applyGrowthPointsUseCase.execute(selectedPlantId, GamificationConstants.GROWTH_POINTS_PER_COMPLETED_FOCUS_SESSION).get();
                            }
                        } else {
                            taskStatisticsRepository.markTaskAsCompletedOnceSync(taskId);
                        }
                    }
                    taskStatisticsRepository.updateCompletionTimeSync(taskId, completionTimeUtc);

                    if (gamificationToUpdate != null) {
                        Gamification finalGamification = new Gamification(
                                gamificationToUpdate.getId(), gamificationToUpdate.getUserId(),
                                gamificationToUpdate.getLevel(), Math.max(0, gamificationToUpdate.getExperience() + finalDeltaXp),
                                Math.max(0, gamificationToUpdate.getCoins() + finalDeltaCoins),
                                gamificationToUpdate.getMaxExperienceForLevel(), completionTimeUtc,
                                gamificationToUpdate.getCurrentStreak(), gamificationToUpdate.getLastClaimedDate(),
                                gamificationToUpdate.getMaxStreak()
                        );
                        gamificationRepository.updateGamificationSync(finalGamification);
                        globalStatisticsRepository.updateLastActiveSync(); // userId используется внутри

                        if (finalDeltaXp != 0 || finalDeltaCoins != 0) {
                            gamificationHistoryRepository.insertHistoryEntrySync(new GamificationHistory(
                                    gamificationId, completionTimeUtc, finalDeltaXp, finalDeltaCoins,
                                    HISTORY_REASON_TASK_COMPLETED, taskId
                            ));
                        }
                    }
                    return null;
                });
                return null;
            } catch (Exception e) {
                logger.error(TAG, "Failed to process task completion for taskId=" + taskId, e);
                throw e;
            }
        }, ioExecutor);
    }
}