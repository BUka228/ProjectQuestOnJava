package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden; // Импорт модели
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class ApplyGrowthPointsUseCase {
    private static final String TAG = "ApplyGrowthPointsUC";
    private static final int MAX_GROWTH_STAGE = 9;
    // Пороги GP для перехода НА СЛЕДУЮЩУЮ стадию (ключ - СЛЕДУЮЩАЯ стадия)
    private static final java.util.Map<Integer, Integer> GROWTH_THRESHOLDS;
    static {
        GROWTH_THRESHOLDS = new java.util.HashMap<>();
        GROWTH_THRESHOLDS.put(1, 50);
        GROWTH_THRESHOLDS.put(2, 120);
        GROWTH_THRESHOLDS.put(3, 250);
        GROWTH_THRESHOLDS.put(4, 450);
        GROWTH_THRESHOLDS.put(5, 700);
        GROWTH_THRESHOLDS.put(6, 1000);
        GROWTH_THRESHOLDS.put(7, 1400);
        GROWTH_THRESHOLDS.put(8, 1900);
        GROWTH_THRESHOLDS.put(9, 2500);
    }

    private final VirtualGardenRepository virtualGardenRepository;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public ApplyGrowthPointsUseCase(
            VirtualGardenRepository virtualGardenRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.virtualGardenRepository = virtualGardenRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(long plantId, int pointsToAdd) {
        return Futures.submitAsync(() -> {
            if (pointsToAdd <= 0) {
                logger.debug(TAG, "Points to add is zero or negative (" + pointsToAdd + "), skipping for plant " + plantId + ".");
                return Futures.immediateFuture(null);
            }
            logger.debug(TAG, "Attempting to add " + pointsToAdd + " GP to plant " + plantId);

            try {
                VirtualGarden currentPlant = Futures.getDone(virtualGardenRepository.getPlant(plantId));
                if (currentPlant == null) {
                    throw new NoSuchElementException("Plant with ID " + plantId + " not found.");
                }

                if (currentPlant.getGrowthStage() >= MAX_GROWTH_STAGE) {
                    logger.debug(TAG, "Plant " + plantId + " is already at max growth stage (" + currentPlant.getGrowthStage() + "). Skipping GP addition.");
                    return Futures.immediateFuture(null);
                }

                int newTotalPoints = currentPlant.getGrowthPoints() + pointsToAdd;
                int potentialNewStage = currentPlant.getGrowthStage();
                boolean stageDidChange = false;

                int nextStage = currentPlant.getGrowthStage() + 1;
                Integer thresholdForNextStage = GROWTH_THRESHOLDS.get(nextStage);

                if (thresholdForNextStage != null && newTotalPoints >= thresholdForNextStage) {
                    potentialNewStage = nextStage;
                    stageDidChange = true;
                    logger.info(TAG, "Plant " + plantId + " reached stage " + potentialNewStage + "! (Threshold: " + thresholdForNextStage + ", Total Points: " + newTotalPoints + ")");
                }

                VirtualGarden updatedPlant = new VirtualGarden(
                        currentPlant.getId(),
                        currentPlant.getGamificationId(),
                        currentPlant.getPlantType(),
                        potentialNewStage,
                        newTotalPoints,
                        currentPlant.getLastWatered() // lastWatered не меняется здесь
                );

                Futures.getDone(virtualGardenRepository.updatePlant(updatedPlant));

                if (stageDidChange) {
                    logger.debug(TAG, "Plant " + plantId + " updated. New stage: " + potentialNewStage + ", New points: " + newTotalPoints);
                } else {
                    logger.debug(TAG, "Plant " + plantId + " points updated to " + newTotalPoints + " (stage unchanged).");
                }
                return Futures.immediateFuture(null);
            } catch (Exception e) {
                logger.error(TAG, "Failed to apply " + pointsToAdd + " GP to plant " + plantId, e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }
}