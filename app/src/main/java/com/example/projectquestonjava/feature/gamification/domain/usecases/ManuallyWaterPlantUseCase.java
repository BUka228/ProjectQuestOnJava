package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class ManuallyWaterPlantUseCase {
    private static final String TAG = "ManuallyWaterPlantUC";
    private static final int POINTS_PER_WATERING = 10;

    private final VirtualGardenRepository virtualGardenRepository;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final ApplyGrowthPointsUseCase applyGrowthPointsUseCase;
    private final UnitOfWork unitOfWork;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public ManuallyWaterPlantUseCase(
            VirtualGardenRepository virtualGardenRepository,
            GamificationDataStoreManager gamificationDataStoreManager,
            ApplyGrowthPointsUseCase applyGrowthPointsUseCase,
            UnitOfWork unitOfWork,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.virtualGardenRepository = virtualGardenRepository;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.applyGrowthPointsUseCase = applyGrowthPointsUseCase;
        this.unitOfWork = unitOfWork;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute() {
        return Futures.submit(() -> {
            logger.debug(TAG, "ManuallyWaterPlantUseCase execute started.");
            final Long gamificationId;
            VirtualGarden latestPlant;

            try {
                gamificationId = gamificationDataStoreManager.getGamificationIdSync();
                if (gamificationId == null || gamificationId == -1L) {
                    throw new IllegalStateException("Gamification ID not found for watering.");
                }
                logger.debug(TAG, "Attempting manual watering for gamificationId " + gamificationId);

                latestPlant = virtualGardenRepository.getLatestPlantSync(gamificationId);

                if (latestPlant == null) {
                    logger.warn(TAG, "No plants found for user " + gamificationId + ". Watering skipped.");
                    return null;
                }
                if (latestPlant.getGamificationId() != gamificationId) {
                    logger.error(TAG, "Mismatch: latestPlant.gamificationId=" + latestPlant.getGamificationId() + " vs current gamificationId=" + gamificationId);
                    throw new IllegalStateException("Plant ownership mismatch for watering operation.");
                }

                LocalDate today = dateTimeUtils.currentLocalDate();
                LocalDateTime lastWateredUtc = latestPlant.getLastWatered();
                LocalDate lastWateredLocalDate = dateTimeUtils.utcToLocalLocalDateTime(lastWateredUtc).toLocalDate();

                if (!lastWateredLocalDate.isBefore(today)) {
                    logger.warn(TAG, "Plants for user " + gamificationId + " already watered today (last watered: " + lastWateredLocalDate + ").");
                    throw new IllegalStateException("Растения уже политы сегодня.");
                }

            } catch (IllegalStateException | IOException e) {
                logger.error(TAG, "Pre-transaction check failed for watering", e);
                throw e;
            }

            try {
                unitOfWork.withTransaction((Callable<Void>) () -> {
                    LocalDateTime nowUtc = dateTimeUtils.currentUtcDateTime();
                    // Используем СИНХРОННЫЙ метод репозитория
                    virtualGardenRepository.updateLastWateredForAllUserPlantsSync(gamificationId, nowUtc);
                    logger.debug(TAG, "Updated lastWatered for all plants of user " + gamificationId + " to " + nowUtc);

                    Long selectedPlantId = gamificationDataStoreManager.getSelectedPlantIdSync();
                    if (selectedPlantId != null && selectedPlantId != -1L) {
                        applyGrowthPointsUseCase.executeSync(selectedPlantId, POINTS_PER_WATERING);
                        logger.debug(TAG, "Applied " + POINTS_PER_WATERING + " GP to selected plant " + selectedPlantId);
                    } else {
                        logger.warn(TAG, "No plant selected, skipping GP application for watering.");
                    }
                    return null;
                });
                logger.info(TAG, "Manual watering successful for user " + gamificationId);
                return null;
            } catch (Exception e) {
                logger.error(TAG, "Transaction failed during manual watering for gamificationId " + gamificationId, e);
                throw e;
            }
        }, ioExecutor);
    }
}