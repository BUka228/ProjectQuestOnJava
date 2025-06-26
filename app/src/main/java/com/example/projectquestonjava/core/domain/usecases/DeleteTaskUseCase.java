package com.example.projectquestonjava.core.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import javax.inject.Inject;

public class DeleteTaskUseCase {
    private static final String TAG = "DeleteTaskUseCase"; // Добавим TAG для логгирования

    private final TaskRepository taskRepository;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public DeleteTaskUseCase(
            TaskRepository taskRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskRepository = taskRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    // Метод execute возвращает ListenableFuture<Void>
    public ListenableFuture<Void> execute(long taskId) {
        // Выполняем операцию на ioExecutor
        return Futures.submitAsync(() -> {
            logger.debug(TAG, "Deleting task " + taskId + ".");
            try {
                ListenableFuture<Void> deleteFuture = taskRepository.deleteTaskById(taskId);
                Futures.getUnchecked(deleteFuture);

                logger.info(TAG, "Task " + taskId + " deleted successfully (via repository).");
                return Futures.immediateFuture(null);
            } catch (Exception e) {
                logger.error(TAG, "Failed to delete task " + taskId, e);
                // Пробрасываем исключение, чтобы ListenableFuture завершился с ошибкой
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }
}