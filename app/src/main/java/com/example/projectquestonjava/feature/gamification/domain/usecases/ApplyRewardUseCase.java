package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantType;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;
import com.example.projectquestonjava.feature.gamification.domain.repository.BadgeRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import javax.inject.Inject;

import lombok.Getter;

public class ApplyRewardUseCase {
    private static final String TAG = "ApplyRewardUseCase";

    private final GamificationRepository gamificationRepository;
    private final BadgeRepository badgeRepository;
    private final VirtualGardenRepository virtualGardenRepository;
    private final Executor ioExecutor;
    private final Logger logger;

    // Класс для возврата результата
    @Getter
    public static class RewardApplicationResult {
        private final int deltaXp;
        private final int deltaCoins;

        public RewardApplicationResult(int deltaXp, int deltaCoins) {
            this.deltaXp = deltaXp;
            this.deltaCoins = deltaCoins;
        }
    }


    @Inject
    public ApplyRewardUseCase(
            GamificationRepository gamificationRepository,
            BadgeRepository badgeRepository,
            VirtualGardenRepository virtualGardenRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.gamificationRepository = gamificationRepository;
        this.badgeRepository = badgeRepository;
        this.virtualGardenRepository = virtualGardenRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<RewardApplicationResult> execute(long gamificationId, Reward reward) {
        return Futures.submitAsync(() -> {
            logger.debug(TAG, "Applying reward " + reward.getId() + " ('" + reward.getName() + "', type=" + reward.getRewardType() + ") for gamificationId " + gamificationId);
            try {
                Gamification currentGamification = Futures.getDone(gamificationRepository.getGamificationById(gamificationId));
                if (currentGamification == null) {
                    throw new IllegalStateException("Gamification data not found for ID " + gamificationId);
                }
                int currentLevel = currentGamification.getLevel();
                int deltaXp = 0;
                int deltaCoins = 0;

                switch (reward.getRewardType()) {
                    case COINS:
                        deltaCoins = calculateValue(reward.getRewardValue(), currentLevel);
                        logger.debug(TAG, "Calculated coin delta: " + deltaCoins);
                        break;
                    case EXPERIENCE:
                        deltaXp = calculateValue(reward.getRewardValue(), currentLevel);
                        logger.debug(TAG, "Calculated experience delta: " + deltaXp);
                        break;
                    case BADGE:
                        Long badgeId = Long.parseLong(reward.getRewardValue()); // Может бросить NumberFormatException
                        Futures.getDone(badgeRepository.insertEarnedBadge(
                                new GamificationBadgeCrossRef(gamificationId, badgeId, LocalDateTime.now())
                        ));
                        logger.debug(TAG, "Applied badge with ID " + badgeId + ".");
                        break;
                    case PLANT:
                        PlantType plantType = PlantType.valueOf(reward.getRewardValue().toUpperCase());
                        Futures.getDone(virtualGardenRepository.insertPlant(
                                new VirtualGarden(gamificationId, plantType, 0, 0, LocalDateTime.now().minusDays(1))
                        ));
                        logger.debug(TAG, "Applied plant reward: " + plantType + ".");
                        break;
                    case THEME:
                        logger.debug(TAG, "Applying theme reward: " + reward.getRewardValue());
                        // Логика themeManager.unlockTheme(...)
                        break;
                }
                return Futures.immediateFuture(new RewardApplicationResult(deltaXp, deltaCoins));
            } catch (Exception e) {
                logger.error(TAG, "Failed to apply reward " + reward.getId() + " ('" + reward.getName() + "')", e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }

    private int calculateValue(String rewardValue, int level) {
        String trimmedValue = rewardValue.trim();
        try {
            if (trimmedValue.toUpperCase().startsWith("LEVEL*")) {
                int multiplier = Integer.parseInt(trimmedValue.substring("LEVEL*".length()));
                return Math.max(0, level * multiplier);
            } else if (trimmedValue.toUpperCase().startsWith("BASE*")) {
                String[] parts = trimmedValue.split("\\*");
                if (parts.length != 3) throw new IllegalArgumentException("Invalid BASE formula: " + trimmedValue);
                int base = Integer.parseInt(parts[1]);
                double factor = Double.parseDouble(parts[2]);
                if (factor < 0) throw new IllegalArgumentException("Factor cannot be negative: " + trimmedValue);
                return Math.max(0, (int) (base * Math.pow(factor, level - 1)));
            } else {
                return Math.max(0, Integer.parseInt(trimmedValue));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value or formula: " + trimmedValue, e);
        }
    }
}