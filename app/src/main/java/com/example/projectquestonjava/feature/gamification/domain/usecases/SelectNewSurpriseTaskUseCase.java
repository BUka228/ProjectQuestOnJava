package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.example.projectquestonjava.feature.gamification.domain.repository.SurpriseTaskRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class SelectNewSurpriseTaskUseCase {
    private static final String TAG = "SelectNewSurpriseTaskUC";

    private final SurpriseTaskRepository surpriseTaskRepository;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final Executor ioExecutor;
    private final Logger logger;
    private final Random random = new Random();

    @Inject
    public SelectNewSurpriseTaskUseCase(
            SurpriseTaskRepository surpriseTaskRepository,
            GamificationDataStoreManager gamificationDataStoreManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.surpriseTaskRepository = surpriseTaskRepository;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<SurpriseTask> execute(LocalDate date) {
        // Получаем gamificationId асинхронно
        ListenableFuture<Long> gamificationIdFuture = gamificationDataStoreManager.getGamificationIdFuture();

        return Futures.transformAsync(gamificationIdFuture, gamificationId -> {
            if (gamificationId == null || gamificationId == -1L) {
                logger.warn(TAG, "Cannot select surprise task, Gamification ID not found.");
                return Futures.immediateFuture(null); // Не можем выбрать без ID
            }

            // 1. Проверяем, есть ли УЖЕ активная задача
            ListenableFuture<SurpriseTask> existingActiveTaskFuture =
                    surpriseTaskRepository.getActiveTaskForDateFuture(gamificationId, date);

            return Futures.transformAsync(existingActiveTaskFuture, existingActiveTask -> {
                if (existingActiveTask != null) {
                    logger.debug(TAG, "Active surprise task " + existingActiveTask.getId() +
                            " already exists for " + date + " for gamiId " + gamificationId);
                    return Futures.immediateFuture(existingActiveTask); // Возвращаем существующую
                }

                // 2. Если активной нет, ищем доступные
                logger.debug(TAG, "No active task found for " + date +
                        ". Attempting to select a new one for GamiID " + gamificationId);

                ListenableFuture<List<SurpriseTask>> availableTasksFuture =
                        surpriseTaskRepository.getAvailableTasks(gamificationId);

                return Futures.transformAsync(availableTasksFuture, availableTasks -> {
                    if (availableTasks == null || availableTasks.isEmpty()) {
                        logger.debug(TAG, "No available (uncompleted, not expired, not shown) surprise tasks found.");
                        return Futures.immediateFuture(null); // Нет доступных
                    }

                    // 3. Выбираем случайную
                    SurpriseTask taskToSelect = availableTasks.get(random.nextInt(availableTasks.size()));
                    logger.info(TAG, "Selected task " + taskToSelect.getId() +
                            " to be shown for today (" + date + ").");

                    // 4. Обновляем задачу - устанавливаем shownDate
                    SurpriseTask updatedTask = new SurpriseTask(
                            taskToSelect.getId(),
                            taskToSelect.getGamificationId(),
                            taskToSelect.getDescription(),
                            taskToSelect.getRewardId(),
                            taskToSelect.getExpirationTime(),
                            taskToSelect.isCompleted(),
                            date // Устанавливаем shownDate
                    );

                    // 5. Сохраняем обновленную задачу
                    return Futures.transform(
                            surpriseTaskRepository.updateSurpriseTask(updatedTask),
                            aVoid -> { // updateSurpriseTask возвращает ListenableFuture<Void>
                                logger.info(TAG, "Task " + updatedTask.getId() +
                                        " successfully marked as shown for today (" + date + ").");
                                return updatedTask; // Возвращаем обновленную задачу
                            },
                            MoreExecutors.directExecutor() // Для простого преобразования Void -> SurpriseTask
                    );
                }, ioExecutor); // transformAsync для availableTasksFuture
            }, ioExecutor); // transformAsync для existingActiveTaskFuture
        }, ioExecutor); // transformAsync для gamificationIdFuture
    }
}