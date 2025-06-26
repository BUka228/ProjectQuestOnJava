package com.example.projectquestonjava.core.domain.usecases.tag;

import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskTagRepository;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import javax.inject.Inject;

public class AddTagUseCase {
    private static final String TAG = "AddTagUseCase";

    private final TaskTagRepository taskTagRepository;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public AddTagUseCase(
            TaskTagRepository taskTagRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskTagRepository = taskTagRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Long> execute(String tagName) {
        return Futures.submitAsync(() -> { // Выполняем на ioExecutor
            if (tagName == null || tagName.trim().isEmpty()) {
                logger.warn(TAG, "Tag name cannot be blank.");
                // Возвращаем Future с ошибкой
                return Futures.immediateFailedFuture(new IllegalArgumentException("Tag name cannot be blank"));
            }
            String trimmedTagName = tagName.trim();
            logger.debug(TAG, "Adding tag with name: " + trimmedTagName);

            Tag newTag = new Tag(trimmedTagName, "#CCCCCC");

            // taskTagRepository.insertTag уже возвращает ListenableFuture<Long>
            return Futures.catchingAsync(
                    taskTagRepository.insertTag(newTag),
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Failed to add tag '" + trimmedTagName + "' via repository", e);
                        throw new RuntimeException("Failed to add tag: " + e.getMessage(), e); // Пробрасываем для ListenableFuture
                    },
                    ioExecutor
            );
        }, ioExecutor);
    }
}