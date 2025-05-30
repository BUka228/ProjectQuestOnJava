package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.data.model.StreakRewardDefinition;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.model.GamificationEvent;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantType;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;
import com.example.projectquestonjava.feature.gamification.domain.repository.BadgeRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.RewardRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.StreakRewardDefinitionRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.GlobalStatisticsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class ClaimDailyRewardUseCase {
    private static final String TAG = "ClaimDailyRewardUseCase";
    private static final long FALLBACK_REWARD_ID_FOR_DUPLICATE_BADGE = 100L;
    private static final long FALLBACK_REWARD_ID_FOR_DUPLICATE_PLANT = 101L;

    private final GamificationRepository gamificationRepository;
    private final StreakRewardDefinitionRepository streakRewardDefinitionRepository;
    private final RewardRepository rewardRepository;
    private final BadgeRepository badgeRepository;
    private final VirtualGardenRepository virtualGardenRepository;
    private final GlobalStatisticsRepository globalStatisticsRepository;
    private final ApplyRewardUseCase applyRewardUseCase;
    private final UpdateChallengeProgressUseCase updateChallengeProgressUseCase;
    private final UnitOfWork unitOfWork;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public ClaimDailyRewardUseCase(
            GamificationRepository gamificationRepository,
            StreakRewardDefinitionRepository streakRewardDefinitionRepository,
            RewardRepository rewardRepository,
            BadgeRepository badgeRepository,
            VirtualGardenRepository virtualGardenRepository,
            GlobalStatisticsRepository globalStatisticsRepository,
            ApplyRewardUseCase applyRewardUseCase,
            UpdateChallengeProgressUseCase updateChallengeProgressUseCase,
            UnitOfWork unitOfWork,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.gamificationRepository = gamificationRepository;
        this.streakRewardDefinitionRepository = streakRewardDefinitionRepository;
        this.rewardRepository = rewardRepository;
        this.badgeRepository = badgeRepository;
        this.virtualGardenRepository = virtualGardenRepository;
        this.globalStatisticsRepository = globalStatisticsRepository;
        this.applyRewardUseCase = applyRewardUseCase;
        this.updateChallengeProgressUseCase = updateChallengeProgressUseCase;
        this.unitOfWork = unitOfWork;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute() {
        logger.debug(TAG, "Attempting to claim daily reward...");
        return Futures.transformAsync(
                gamificationRepository.getCurrentUserGamificationFuture(),
                gamification -> {
                    if (gamification == null) {
                        return Futures.immediateFailedFuture(new IllegalStateException("Gamification profile not found for current user."));
                    }
                    final long gamificationId = gamification.getId();
                    int currentStreak = gamification.getCurrentStreak();
                    LocalDate lastClaimed = gamification.getLastClaimedDate();
                    LocalDate today = dateTimeUtils.currentLocalDate();

                    if (!lastClaimed.isBefore(today)) {
                        logger.warn(TAG, "Reward already claimed today (" + lastClaimed + "). Cannot claim again.");
                        return Futures.immediateFailedFuture(new IllegalStateException("Reward already claimed today."));
                    }

                    long daysDifference = ChronoUnit.DAYS.between(lastClaimed, today);
                    final int newStreak = (daysDifference == 1L) ? currentStreak + 1 : 1;
                    logger.debug(TAG, "Calculated new streak: " + newStreak);

                    return Futures.transformAsync(
                            streakRewardDefinitionRepository.getRewardDefinitionForStreak(newStreak),
                            definition -> {
                                if (definition == null) {
                                    return Futures.immediateFailedFuture(new IllegalStateException("Reward definition not found for streak day " + newStreak + "."));
                                }
                                return Futures.transformAsync(
                                        rewardRepository.getRewardById(definition.getRewardId()),
                                        baseReward -> {
                                            if (baseReward == null) {
                                                return Futures.immediateFailedFuture(new IllegalStateException("Reward ID " + definition.getRewardId() + " not found."));
                                            }
                                            return Futures.transformAsync(
                                                    checkAndGetFinalReward(gamificationId, baseReward), // Этот метод уже возвращает ListenableFuture<Reward>
                                                    finalRewardToApply -> {
                                                        if (finalRewardToApply == null) {
                                                            return Futures.immediateFailedFuture(new IllegalStateException("Final reward to apply is null."));
                                                        }
                                                        // Выполнение транзакции
                                                        return Futures.submit(() -> { // Оборачиваем транзакцию в submit, чтобы она выполнилась на ioExecutor
                                                            unitOfWork.withTransaction((Callable<Void>) () -> {
                                                                ApplyRewardUseCase.RewardApplicationResult rewardResult = applyRewardUseCase.execute(gamificationId, finalRewardToApply); // СИНХРОННЫЙ
                                                                int deltaXp = rewardResult.getDeltaXp();
                                                                int deltaCoins = rewardResult.getDeltaCoins();

                                                                Gamification gamificationForUpdate = gamificationRepository.getGamificationByIdSync(gamificationId); // СИНХРОННЫЙ
                                                                if (gamificationForUpdate == null) throw new IllegalStateException("Gamification profile disappeared.");

                                                                Gamification updatedGamification = new Gamification(
                                                                        gamificationForUpdate.getId(), gamificationForUpdate.getUserId(),
                                                                        gamificationForUpdate.getLevel(), Math.max(0, gamificationForUpdate.getExperience() + deltaXp),
                                                                        Math.max(0, gamificationForUpdate.getCoins() + deltaCoins),
                                                                        gamificationForUpdate.getMaxExperienceForLevel(), dateTimeUtils.currentUtcDateTime(),
                                                                        newStreak, today, Math.max(gamificationForUpdate.getMaxStreak(), newStreak)
                                                                );
                                                                gamificationRepository.updateGamificationSync(updatedGamification); // СИНХРОННЫЙ
                                                                globalStatisticsRepository.updateLastActiveSync(); // СИНХРОННЫЙ

                                                                GamificationEvent event = new GamificationEvent.StreakUpdated(newStreak);
                                                                updateChallengeProgressUseCase.executeSync(gamificationId, event); // СИНХРОННЫЙ
                                                                logger.info(TAG, "Daily reward " + finalRewardToApply.getId() + " ('" + finalRewardToApply.getName() + "') claimed successfully.");
                                                                return null;
                                                            });
                                                            return null; // для ListenableFuture<Void>
                                                        }, ioExecutor);
                                                    },
                                                    ioExecutor
                                            );
                                        },
                                        ioExecutor
                                );
                            },
                            ioExecutor
                    );
                },
                ioExecutor
        );
    }

    private ListenableFuture<Reward> checkAndGetFinalReward(long gamificationId, Reward baseReward) {
        if (baseReward.getRewardType() == RewardType.BADGE) {
            long badgeId;
            try { badgeId = Long.parseLong(baseReward.getRewardValue()); }
            catch (NumberFormatException e) { return Futures.immediateFuture(baseReward); }

            return Futures.transformAsync(badgeRepository.getEarnedBadges(), earnedBadges -> { // getEarnedBadges для текущего пользователя
                boolean alreadyHasBadge = earnedBadges != null && earnedBadges.stream().anyMatch(ref -> ref.getBadgeId() == badgeId && ref.getGamificationId() == gamificationId);
                if (alreadyHasBadge) {
                    return rewardRepository.getRewardById(FALLBACK_REWARD_ID_FOR_DUPLICATE_BADGE);
                }
                return Futures.immediateFuture(baseReward);
            }, ioExecutor);
        } else if (baseReward.getRewardType() == RewardType.PLANT) {
            PlantType plantTypeToAward;
            try { plantTypeToAward = PlantType.valueOf(baseReward.getRewardValue().toUpperCase());}
            catch (IllegalArgumentException e) { return Futures.immediateFuture(baseReward); }

            return Futures.transformAsync(virtualGardenRepository.getAllPlantsFuture(), userPlants -> { // getAllPlantsFuture для текущего пользователя
                boolean alreadyHasPlant = userPlants != null && userPlants.stream().anyMatch(p -> p.getPlantType() == plantTypeToAward && p.getGamificationId() == gamificationId);
                if (alreadyHasPlant) {
                    return rewardRepository.getRewardById(FALLBACK_REWARD_ID_FOR_DUPLICATE_PLANT);
                }
                return Futures.immediateFuture(baseReward);
            }, ioExecutor);
        }
        return Futures.immediateFuture(baseReward);
    }
}