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
    private static final long FALLBACK_REWARD_ID_FOR_DUPLICATE_BADGE = 100L; // Пример ID для запасной награды за значок
    private static final long FALLBACK_REWARD_ID_FOR_DUPLICATE_PLANT = 101L; // Пример ID для запасной награды за растение
    // (Убедитесь, что эти ID существуют в вашей таблице Reward)


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
        return Futures.submitAsync(() -> { // Вся логика в submitAsync
            logger.debug(TAG, "Attempting to claim daily reward...");
            try {
                // 1. Получаем текущий профиль геймификации
                // Предполагается, что getCurrentUserGamificationFuture() существует в GamificationRepository
                Gamification gamification = Futures.getDone(gamificationRepository.getCurrentUserGamificationFuture());
                if (gamification == null) {
                    throw new IllegalStateException("Gamification profile not found for current user.");
                }
                final long gamificationId = gamification.getId(); // final для использования в лямбдах
                int currentStreak = gamification.getCurrentStreak();
                LocalDate lastClaimed = gamification.getLastClaimedDate();
                LocalDate today = dateTimeUtils.currentLocalDate();

                // 2. Проверяем, можно ли получить награду сегодня
                if (!lastClaimed.isBefore(today)) {
                    logger.warn(TAG, "Reward already claimed today (" + lastClaimed + "). Cannot claim again.");
                    throw new IllegalStateException("Reward already claimed today.");
                }

                // 3. Рассчитываем новый стрик и день для запроса награды
                long daysDifference = ChronoUnit.DAYS.between(lastClaimed, today);
                int newStreak = (daysDifference == 1L) ? currentStreak + 1 : 1;
                logger.debug(TAG, "Calculated new streak: " + newStreak + " (Streak day for reward: " + newStreak + ")");

                // 4. Получаем определение награды
                StreakRewardDefinition definition = Futures.getDone(streakRewardDefinitionRepository.getRewardDefinitionForStreak(newStreak));
                if (definition == null) {
                    throw new IllegalStateException("Reward definition not found for streak day " + newStreak + ".");
                }

                // 5. Получаем основную награду
                long rewardIdToProcess = definition.getRewardId();
                Reward baseReward = Futures.getDone(rewardRepository.getRewardById(rewardIdToProcess));
                if (baseReward == null) {
                    throw new IllegalStateException("Reward ID " + rewardIdToProcess + " not found for streak day " + newStreak + ".");
                }

                // 6. Проверка дубликатов и выбор запасной награды (асинхронно)
                ListenableFuture<Reward> finalRewardFuture = checkAndGetFinalReward(gamificationId, baseReward);

                // 7. Применяем награду, обновляем Gamification, статистику и челленджи ВНУТРИ ТРАНЗАКЦИИ
                return Futures.transformAsync(finalRewardFuture, finalRewardToApply -> {
                    if (finalRewardToApply == null) { // Это может произойти, если fallback награда не найдена
                        throw new IllegalStateException("Final reward to apply is null, possibly due to missing fallback reward.");
                    }
                    try {
                        unitOfWork.withTransaction((Callable<Void>) () -> {
                            // 7.1 Применяем выбранную награду
                            ApplyRewardUseCase.RewardApplicationResult rewardResult = Futures.getDone((java.util.concurrent.Future<ApplyRewardUseCase.RewardApplicationResult>) applyRewardUseCase.execute(gamificationId, finalRewardToApply));
                            int deltaXp = rewardResult.getDeltaXp();
                            int deltaCoins = rewardResult.getDeltaCoins();
                            logger.debug(TAG, "Applied reward '" + finalRewardToApply.getName() + "'. Delta(XP/Coins): (" + deltaXp + "/" + deltaCoins + ")");

                            // 7.2 Обновляем объект Gamification (получаем его снова, чтобы иметь самую свежую версию перед обновлением)
                            Gamification gamificationForUpdate = Futures.getDone(gamificationRepository.getGamificationById(gamificationId));
                            if (gamificationForUpdate == null) throw new IllegalStateException("Gamification profile disappeared during transaction.");

                            Gamification updatedGamification = new Gamification(
                                    gamificationForUpdate.getId(), gamificationForUpdate.getUserId(),
                                    gamificationForUpdate.getLevel(), // Уровень обновится в gamificationRepository.updateGamification
                                    Math.max(0, gamificationForUpdate.getExperience() + deltaXp),
                                    Math.max(0, gamificationForUpdate.getCoins() + deltaCoins),
                                    gamificationForUpdate.getMaxExperienceForLevel(), // Обновится в gamificationRepository.updateGamification
                                    dateTimeUtils.currentUtcDateTime(), // lastActive
                                    newStreak, // newStreak
                                    today, // lastClaimedDate
                                    Math.max(gamificationForUpdate.getMaxStreak(), newStreak) // maxStreak
                            );

                            // 7.3 Сохраняем обновленный Gamification
                            Futures.getDone(gamificationRepository.updateGamification(updatedGamification));
                            logger.debug(TAG, "Gamification state updated in repository. New streak: " + newStreak);

                            // 7.4 Обновляем время последней активности в глобальной статистике
                            Futures.getDone(globalStatisticsRepository.updateLastActive());
                            logger.debug(TAG, "Updated global last active time.");

                            // 7.5 Обновляем прогресс челленджей, связанных со стриком
                            GamificationEvent event = new GamificationEvent.StreakUpdated(newStreak);
                            Futures.getDone(updateChallengeProgressUseCase.execute(gamificationId, event, ioExecutor));
                            logger.debug(TAG, "Processed challenge progress for streak update (" + newStreak + ").");

                            logger.info(TAG, "Daily reward " + finalRewardToApply.getId() + " ('" + finalRewardToApply.getName() + "') for streak day " + newStreak + " claimed successfully.");
                            return null; // для Callable<Void>
                        });
                        return Futures.immediateFuture(null); // Успешное завершение транзакции
                    } catch (Exception e) {
                        logger.error(TAG, "Transaction failed while claiming daily reward", e);
                        return Futures.immediateFailedFuture(e);
                    }
                }, ioExecutor); // transformAsync для finalRewardFuture
            } catch (Exception e) {
                if (e instanceof IllegalStateException && "Reward already claimed today.".equals(e.getMessage())) {
                    logger.warn(TAG, e.getMessage());
                } else {
                    logger.error(TAG, "Failed to claim daily reward", e);
                }
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor); // submitAsync для всей логики
    }

    private ListenableFuture<Reward> checkAndGetFinalReward(long gamificationId, Reward baseReward) {
        if (baseReward.getRewardType() == RewardType.BADGE) {
            long badgeId;
            try {
                badgeId = Long.parseLong(baseReward.getRewardValue());
            } catch (NumberFormatException e) {
                logger.error(TAG, "Invalid badge ID in reward value: " + baseReward.getRewardValue(), e);
                return Futures.immediateFuture(baseReward); // Возвращаем исходную, но это ошибка конфигурации
            }
            ListenableFuture<List<GamificationBadgeCrossRef>> earnedBadgesFuture = badgeRepository.getEarnedBadges(); // Для текущего пользователя
            return Futures.transformAsync(earnedBadgesFuture, earnedBadges -> {
                boolean alreadyHasBadge = earnedBadges != null && earnedBadges.stream().anyMatch(ref -> ref.getBadgeId() == badgeId);
                if (alreadyHasBadge) {
                    logger.info(TAG, "User already has badge " + badgeId + ". Applying fallback reward ID " + FALLBACK_REWARD_ID_FOR_DUPLICATE_BADGE);
                    return rewardRepository.getRewardById(FALLBACK_REWARD_ID_FOR_DUPLICATE_BADGE);
                }
                return Futures.immediateFuture(baseReward);
            }, ioExecutor);
        } else if (baseReward.getRewardType() == RewardType.PLANT) {
            PlantType plantTypeToAward;
            try {
                plantTypeToAward = PlantType.valueOf(baseReward.getRewardValue().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error(TAG, "Invalid plant type in reward value: " + baseReward.getRewardValue(), e);
                return Futures.immediateFuture(baseReward);
            }
            // VirtualGardenRepository.getAllPlantsFlow() возвращает LiveData, нам нужен ListenableFuture
            // Предположим, есть метод virtualGardenRepository.getAllPlantsFuture() для текущего пользователя
            ListenableFuture<List<VirtualGarden>> userPlantsFuture = virtualGardenRepository.getAllPlantsFuture(); // TODO: Добавить этот метод
            return Futures.transformAsync(userPlantsFuture, userPlants -> {
                boolean alreadyHasPlant = userPlants != null && userPlants.stream().anyMatch(p -> p.getPlantType() == plantTypeToAward);
                if (alreadyHasPlant) {
                    logger.info(TAG, "User already has plant " + plantTypeToAward + ". Applying fallback reward ID " + FALLBACK_REWARD_ID_FOR_DUPLICATE_PLANT);
                    return rewardRepository.getRewardById(FALLBACK_REWARD_ID_FOR_DUPLICATE_PLANT);
                }
                return Futures.immediateFuture(baseReward);
            }, ioExecutor);
        }
        return Futures.immediateFuture(baseReward);
    }
}