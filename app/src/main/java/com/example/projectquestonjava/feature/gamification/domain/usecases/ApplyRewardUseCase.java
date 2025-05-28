package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher; // Не используется для Sync, но может остаться для конструктора
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
// Guava Futures больше не нужны здесь, если метод синхронный
import java.time.LocalDateTime;
import java.util.concurrent.Executor; // Не используется для Sync
import javax.inject.Inject;
import lombok.Getter;

public class ApplyRewardUseCase {
    private static final String TAG = "ApplyRewardUseCase";

    private final GamificationRepository gamificationRepository; // Ожидает getGamificationByIdSync
    private final BadgeRepository badgeRepository;             // Ожидает insertEarnedBadgeSync
    private final VirtualGardenRepository virtualGardenRepository; // Ожидает insertPlantSync
    private final Logger logger;

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
            @IODispatcher Executor ioExecutor, // Может быть не нужен, если все операции синхронны
            Logger logger) {
        this.gamificationRepository = gamificationRepository;
        this.badgeRepository = badgeRepository;
        this.virtualGardenRepository = virtualGardenRepository;
        this.logger = logger;
    }

    // Метод теперь СИНХРОННЫЙ и вызывается из потока, управляемого транзакцией
    public RewardApplicationResult execute(long gamificationId, Reward reward) throws Exception {
        logger.debug(TAG, "SYNC Applying reward " + reward.getId() + " ('" + reward.getName() + "', type=" + reward.getRewardType() + ") for gamificationId " + gamificationId);
        try {
            Gamification currentGamification = gamificationRepository.getGamificationByIdSync(gamificationId);
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
                    Long badgeId = Long.parseLong(reward.getRewardValue());
                    // TODO: Проверить, есть ли значок уже, если insertEarnedBadgeSync не делает OnConflict.IGNORE
                    badgeRepository.insertEarnedBadgeSync(
                            new GamificationBadgeCrossRef(gamificationId, badgeId, LocalDateTime.now())
                    );
                    logger.debug(TAG, "Applied badge with ID " + badgeId + ".");
                    break;
                case PLANT:
                    PlantType plantType = PlantType.valueOf(reward.getRewardValue().toUpperCase());
                    // TODO: Проверить, есть ли такое растение уже
                    virtualGardenRepository.insertPlantSync(
                            new VirtualGarden(gamificationId, plantType, 0, 0, LocalDateTime.now().minusDays(1))
                    );
                    logger.debug(TAG, "Applied plant reward: " + plantType + ".");
                    break;
                case THEME:
                    logger.debug(TAG, "Applying theme reward (not implemented in DB): " + reward.getRewardValue());
                    break;
            }
            return new RewardApplicationResult(deltaXp, deltaCoins);
        } catch (Exception e) {
            logger.error(TAG, "SYNC Failed to apply reward " + reward.getId() + " ('" + reward.getName() + "')", e);
            throw e; // Пробрасываем для обработки внешней транзакцией
        }
    }

    private int calculateValue(String rewardValue, int level) {
        // ... (реализация без изменений) ...
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