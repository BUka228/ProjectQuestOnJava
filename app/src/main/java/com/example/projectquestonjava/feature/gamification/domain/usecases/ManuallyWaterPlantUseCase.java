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
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
        return Futures.submitAsync(() -> {
            try {
                // Получаем gamificationId асинхронно
                Long gamificationId = Futures.getDone(gamificationDataStoreManager.getGamificationIdFuture());
                if (gamificationId == null || gamificationId == -1L) {
                    throw new IllegalStateException("Gamification ID not found for watering.");
                }
                logger.debug(TAG, "Attempting manual watering for gamificationId " + gamificationId);

                // Получаем latestPlant асинхронно
                VirtualGarden latestPlant = Futures.getDone(virtualGardenRepository.getLatestPlant());
                // getLatestPlant() уже должен использовать gamificationId из DataStore,
                // но если gamificationId только что получен, то getLatestPlant() может использовать старый.
                // Это потенциальная проблема, если gamificationId меняется часто.
                // Для большей надежности, можно передавать gamificationId в getLatestPlant.
                // Пока оставим так, предполагая, что gamificationId стабилен на время операции.

                LocalDate today = dateTimeUtils.currentLocalDate();

                if (latestPlant == null) {
                    logger.warn(TAG, "No plants found for user " + gamificationId + ". Watering skipped.");
                    return Futures.immediateFuture(null); // Успешное завершение, т.к. нечего поливать
                }

                LocalDateTime lastWateredUtc = latestPlant.getLastWatered();
                LocalDate lastWateredLocalDate = dateTimeUtils.utcToLocalLocalDateTime(lastWateredUtc).toLocalDate();

                if (!lastWateredLocalDate.isBefore(today)) { // Если дата полива НЕ раньше сегодняшней
                    logger.warn(TAG, "Plants for user " + gamificationId + " already watered today (last watered: " + lastWateredLocalDate + ").");
                    throw new IllegalStateException("Растения уже политы сегодня.");
                }

                // Выполняем операции в транзакции
                unitOfWork.withTransaction((Callable<Void>) () -> {
                    LocalDateTime nowUtc = dateTimeUtils.currentUtcDateTime();

                    // 1. Обновляем lastWatered у ВСЕХ растений
                    Futures.getDone(virtualGardenRepository.updateLastWateredForAllUserPlants(gamificationId, nowUtc));
                    logger.debug(TAG, "Updated lastWatered for all plants of user " + gamificationId + " to " + nowUtc);

                    // 2. Добавляем GP выбранному растению
                    Long selectedPlantId = Futures.getDone(gamificationDataStoreManager.getSelectedPlantIdFuture());
                    if (selectedPlantId != null && selectedPlantId != -1L) {
                        Futures.getDone(applyGrowthPointsUseCase.execute(selectedPlantId, POINTS_PER_WATERING));
                        logger.debug(TAG, "Applied " + POINTS_PER_WATERING + " GP to selected plant " + selectedPlantId);
                    } else {
                        logger.warn(TAG, "No plant selected, skipping GP application for watering.");
                    }
                    return null; // Для Callable<Void>
                });

                logger.info(TAG, "Manual watering successful for user " + gamificationId);
                return Futures.immediateFuture(null);

            } catch (Exception e) {
                // Проверяем, является ли исключение IllegalStateException "Растения уже политы сегодня."
                // и не логируем его как ошибку, а просто пробрасываем.
                if (e instanceof IllegalStateException && "Растения уже политы сегодня.".equals(e.getMessage())) {
                    logger.warn(TAG, e.getMessage());
                } else {
                    logger.error(TAG, "Failed to perform manual watering for gamificationId (unknown)", e);
                }
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }
}