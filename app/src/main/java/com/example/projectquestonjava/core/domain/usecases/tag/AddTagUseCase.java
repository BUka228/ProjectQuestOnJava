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
    private final Executor ioExecutor; // Используем Executor
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

            // Можно добавить проверку на существующий тег здесь, если это критично
            // и TaskTagRepository.insertTag не делает это (хотя OnConflictStrategy.IGNORE в DAO должен помочь)

            Tag newTag = new Tag(trimmedTagName, "#CCCCCC"); // Серый по умолчанию

            // taskTagRepository.insertTag уже возвращает ListenableFuture<Long>
            return Futures.catchingAsync(
                    taskTagRepository.insertTag(newTag),
                    Exception.class, // Ловим любые исключения от DAO
                    e -> {
                        logger.error(TAG, "Failed to add tag '" + trimmedTagName + "' via repository", e);
                        throw new RuntimeException("Failed to add tag: " + e.getMessage(), e); // Пробрасываем для ListenableFuture
                    },
                    ioExecutor // Коллбэк выполняется на том же ioExecutor
            );
        }, ioExecutor); // submitAsync выполняется на ioExecutor
    }
}