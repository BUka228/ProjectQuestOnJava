package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors; // Для directExecutor

import java.util.NoSuchElementException; // Для исключения
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class GrowPlantUseCase {
    private static final String TAG = "GrowPlantUseCase";
    private static final int MAX_GROWTH_STAGE = 5; // Пример, как в Kotlin

    private final VirtualGardenRepository virtualGardenRepository;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public GrowPlantUseCase(
            VirtualGardenRepository virtualGardenRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.virtualGardenRepository = virtualGardenRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    // Метод теперь возвращает ListenableFuture<Void>
    public ListenableFuture<Void> execute(long gamificationId) { // gamificationId теперь передается
        logger.debug(TAG, "Attempting to grow plant for gamificationId " + gamificationId);

        // Вместо прямого вызова getLatestPlant, который для текущего пользователя,
        // нам нужен способ получить latestPlant по gamificationId.
        // Если в VirtualGardenRepository нет такого метода, его нужно добавить.
        // Предположим, такой метод есть: virtualGardenRepository.getLatestPlantByGamificationId(gamificationId)
        // Либо, если UseCase всегда работает с "текущим" пользователем, то gamificationId не нужен,
        // а внутри используется virtualGardenRepository.getLatestPlant() (который берет gamiId из DataStore).
        // Давайте исходить из того, что getLatestPlant() уже работает для текущего пользователя.

        ListenableFuture<VirtualGarden> latestPlantFuture = virtualGardenRepository.getLatestPlant();

        return Futures.transformAsync(latestPlantFuture, latestPlant -> {
            if (latestPlant != null) {
                // Проверяем, принадлежит ли растение этому gamificationId, если это важно
                // (getLatestPlant() должен возвращать для текущего пользователя, чей gamificationId и есть переданный)
                if (latestPlant.getGamificationId() != gamificationId && gamificationId != -1L /* Если -1L, то это может быть общий вызов */) {
                    logger.warn(TAG, "Latest plant gamificationId " + latestPlant.getGamificationId() + " does not match target " + gamificationId);
                    return Futures.immediateFuture(null); // Или ошибка
                }


                if (latestPlant.getGrowthStage() < MAX_GROWTH_STAGE) {
                    VirtualGarden updatedPlant = new VirtualGarden(
                            latestPlant.getId(),
                            latestPlant.getGamificationId(),
                            latestPlant.getPlantType(),
                            latestPlant.getGrowthStage() + 1,
                            latestPlant.getGrowthPoints(), // Очки роста не меняются здесь
                            latestPlant.getLastWatered()
                    );
                    // updatePlant возвращает ListenableFuture<Void>
                    ListenableFuture<Void> updateFuture = virtualGardenRepository.updatePlant(updatedPlant);
                    return Futures.transform(updateFuture, v -> {
                        logger.info(TAG, "Plant " + latestPlant.getId() + " grew to stage " + updatedPlant.getGrowthStage() + " for gamificationId " + gamificationId);
                        return null; // Для ListenableFuture<Void>
                    }, MoreExecutors.directExecutor());
                } else {
                    logger.debug(TAG, "Plant " + latestPlant.getId() + " is already at max growth stage for gamificationId " + gamificationId);
                    return Futures.immediateFuture(null);
                }
            } else {
                logger.warn(TAG, "No plant found for gamification ID " + gamificationId + " to grow.");
                return Futures.immediateFuture(null); // Или бросить исключение, если растение обязательно должно быть
            }
        }, ioExecutor); // Выполняем transformAsync на ioExecutor
    }
}