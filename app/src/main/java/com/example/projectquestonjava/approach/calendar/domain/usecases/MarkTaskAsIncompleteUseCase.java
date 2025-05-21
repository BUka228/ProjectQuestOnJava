package com.example.projectquestonjava.approach.calendar.domain.usecases;

import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class MarkTaskAsIncompleteUseCase {
    private static final String TAG = "MarkTaskAsIncompleteUseCase";

    private final TaskRepository taskRepository;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public MarkTaskAsIncompleteUseCase(
            TaskRepository taskRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskRepository = taskRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(long taskId) {
        logger.debug(TAG, "Marking task " + taskId + " as incomplete.");
        // TaskRepository.updateTaskStatus уже выполняется асинхронно и возвращает ListenableFuture
        return Futures.catchingAsync(
                taskRepository.updateTaskStatus(taskId, TaskStatus.TODO),
                Exception.class,
                e -> {
                    logger.error(TAG, "Failed to mark task " + taskId + " as incomplete", e);
                    throw new RuntimeException("Failed to mark task incomplete", e);
                },
                ioExecutor
        );
    }
}