package com.example.projectquestonjava.core.domain.usecases.tag;

import android.os.Build;

import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskTagRepository;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class DeleteTagsUseCase {
    private static final String TAG = "DeleteTagsUseCase";

    private final TaskTagRepository taskTagRepository;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public DeleteTagsUseCase(
            TaskTagRepository taskTagRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.taskTagRepository = taskTagRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(List<Tag> tagsToDelete) {
        return Futures.submitAsync(() -> { // Выполняем на ioExecutor
            if (tagsToDelete == null || tagsToDelete.isEmpty()) {
                logger.debug(TAG, "No tags to delete.");
                return Futures.immediateFuture(null); // Успешное завершение, ничего не делаем
            }

            List<Long> tagIds;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                tagIds = tagsToDelete.stream().map(Tag::getId).toList();
            } else {
                tagIds = null;
            }
            logger.debug(TAG, "Deleting " + tagsToDelete.size() + " tags with ids: " + tagIds);

            return Futures.catchingAsync(
                    taskTagRepository.deleteTags(tagsToDelete),
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Failed to delete tags with ids: " + tagIds, e);
                        throw new RuntimeException("Failed to delete tags: " + e.getMessage(), e); // Пробрасываем
                    },
                    ioExecutor
            );
        }, ioExecutor);
    }
}