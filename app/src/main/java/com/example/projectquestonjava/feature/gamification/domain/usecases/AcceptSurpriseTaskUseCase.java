package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.RewardRepository;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification; // Убедимся что Gamification.java на месте
import com.example.projectquestonjava.feature.gamification.domain.repository.SurpriseTaskRepository;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class AcceptSurpriseTaskUseCase {
    private static final String TAG = "AcceptSurpriseTaskUC";

    private final SurpriseTaskRepository surpriseTaskRepository;
    private final GamificationRepository gamificationRepository;
    private final RewardRepository rewardRepository;
    private final ApplyRewardUseCase applyRewardUseCase;
    private final UnitOfWork unitOfWork;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public AcceptSurpriseTaskUseCase(
            SurpriseTaskRepository surpriseTaskRepository,
            GamificationRepository gamificationRepository,
            RewardRepository rewardRepository,
            ApplyRewardUseCase applyRewardUseCase,
            UnitOfWork unitOfWork,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.surpriseTaskRepository = surpriseTaskRepository;
        this.gamificationRepository = gamificationRepository;
        this.rewardRepository = rewardRepository;
        this.applyRewardUseCase = applyRewardUseCase;
        this.unitOfWork = unitOfWork;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(SurpriseTask task) {
        return Futures.submitAsync(() -> {
            logger.debug(TAG, "Attempting to accept surprise task " + task.getId());

            if (LocalDateTime.now().isAfter(task.getExpirationTime())) {
                logger.warn(TAG, "Cannot accept task " + task.getId() + ": expiration time has passed (" + task.getExpirationTime() + ").");
                return Futures.immediateFailedFuture(new IllegalStateException("Время для принятия задачи истекло."));
            }
            if (task.isCompleted()) {
                logger.warn(TAG, "Cannot accept task " + task.getId() + ": task is already marked as completed.");
                return Futures.immediateFailedFuture(new IllegalStateException("Задача уже была принята/выполнена."));
            }

            try {
                unitOfWork.withTransaction(() -> { // Callable<Void>
                    // Шаг 1: Получаем награду
                    Reward reward = Futures.getDone(rewardRepository.getRewardById(task.getRewardId()));
                    if (reward == null) {
                        throw new IllegalStateException("Reward not found for surprise task " + task.getId());
                    }
                    logger.debug(TAG, "Found reward '" + reward.getName() + "' for task " + task.getId());

                    // Шаг 2: Получаем текущий профиль геймификации
                    Gamification currentGamification = Futures.getDone(gamificationRepository.getGamificationById(task.getGamificationId()));
                    if (currentGamification == null) {
                        throw new IllegalStateException("Gamification profile not found for id " + task.getGamificationId());
                    }
                    logger.debug(TAG, "Current gamification state: Level " + currentGamification.getLevel() + ", XP " + currentGamification.getExperience());

                    // Шаг 3: Применяем награду
                    ApplyRewardUseCase.RewardApplicationResult rewardResult = Futures.getDone((java.util.concurrent.Future<ApplyRewardUseCase.RewardApplicationResult>) applyRewardUseCase.execute(task.getGamificationId(), reward));
                    int deltaXp = rewardResult.getDeltaXp();
                    int deltaCoins = rewardResult.getDeltaCoins();
                    logger.debug(TAG, "Applied reward. Delta(XP/Coins): (" + deltaXp + "/" + deltaCoins + ")");

                    // Шаг 4: Обновляем профиль геймификации
                    Gamification updatedGamification = new Gamification(
                            currentGamification.getId(),
                            currentGamification.getUserId(),
                            currentGamification.getLevel(), // Уровень пересчитается в репозитории
                            Math.max(0, currentGamification.getExperience() + deltaXp),
                            Math.max(0, currentGamification.getCoins() + deltaCoins),
                            currentGamification.getMaxExperienceForLevel(),
                            currentGamification.getLastActive(), // Время активности обновится в репо
                            currentGamification.getCurrentStreak(),
                            currentGamification.getLastClaimedDate(),
                            currentGamification.getMaxStreak()
                    );
                    Futures.getDone(gamificationRepository.updateGamification(updatedGamification));
                    logger.debug(TAG, "Gamification profile updated.");

                    // Шаг 5: Обновляем статус задачи-сюрприза
                    SurpriseTask completedTask = new SurpriseTask(
                            task.getId(), task.getGamificationId(), task.getDescription(), task.getRewardId(),
                            task.getExpirationTime(), true, task.getShownDate()
                    );
                    Futures.getDone(surpriseTaskRepository.updateSurpriseTask(completedTask));
                    logger.info(TAG, "Surprise task " + task.getId() + " marked as completed.");
                    return null; // для Callable<Void>
                });
                logger.info(TAG, "Surprise task " + task.getId() + " accepted and processed successfully.");
                return Futures.immediateFuture(null);
            } catch (Exception e) {
                logger.error(TAG, "Failed to accept surprise task " + task.getId(), e);
                return Futures.immediateFailedFuture(new RuntimeException("Не удалось принять задачу-сюрприз. " + e.getMessage(), e));
            }
        }, ioExecutor);
    }
}