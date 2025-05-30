package com.example.projectquestonjava.feature.profile.domain.usecases;

import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.AvatarStorageHelper;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors; // Для directExecutor, если нужен

import java.util.concurrent.Executor;
import javax.inject.Inject;

public class RemoveAvatarUseCase {
    private static final String TAG = "RemoveAvatarUseCase";

    private final UserAuthRepository userAuthRepository;
    private final UserSessionManager userSessionManager;
    private final AvatarStorageHelper avatarStorageHelper;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public RemoveAvatarUseCase(
            UserAuthRepository userAuthRepository,
            UserSessionManager userSessionManager,
            AvatarStorageHelper avatarStorageHelper,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.userAuthRepository = userAuthRepository;
        this.userSessionManager = userSessionManager;
        this.avatarStorageHelper = avatarStorageHelper;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute() {
        final int userId = userSessionManager.getUserIdSync();
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.error(TAG, "Cannot remove avatar: User not logged in.");
            return Futures.immediateFailedFuture(new IllegalStateException("Пользователь не авторизован."));
        }
        logger.debug(TAG, "Attempting to remove avatar for userId=" + userId);

        // 1. Получаем текущего пользователя
        ListenableFuture<UserAuth> userFuture = userAuthRepository.getUserById(userId);

        return Futures.transformAsync(userFuture, currentUser -> {
            if (currentUser == null) {
                // Это не должно произойти, если userId валиден, но на всякий случай
                logger.warn(TAG, "User with ID " + userId + " not found. Cannot determine old avatar path.");
                return Futures.immediateFuture(null); // Или ошибка, если пользователь должен существовать
            }
            String oldAvatarPath = currentUser.getAvatarUrl();

            if (oldAvatarPath == null || oldAvatarPath.trim().isEmpty()) {
                logger.debug(TAG, "Avatar is already null for userId=" + userId + ". No action needed.");
                return Futures.immediateFuture(null);
            }

            // 2. Обновляем путь в репозитории на null
            ListenableFuture<Void> updateRepoFuture = userAuthRepository.updateAvatarUrl(userId, null);

            return Futures.transformAsync(updateRepoFuture, aVoid -> {
                logger.info(TAG, "Avatar path set to null in repository for userId=" + userId);
                // 3. Удаляем старый файл
                // avatarStorageHelper.deleteAvatar возвращает ListenableFuture<Void>
                // Мы можем его дождаться или просто запустить
                ListenableFuture<Void> deleteFileFuture = avatarStorageHelper.deleteAvatar(oldAvatarPath);
                return Futures.transform(deleteFileFuture, input -> {
                    logger.info(TAG, "Avatar successfully removed (or attempt was made) for userId=" + userId + ".");
                    return null; // Для ListenableFuture<Void>
                }, ioExecutor); // Можно использовать directExecutor, если deleteAvatar быстрый
            }, ioExecutor);
        }, ioExecutor);
    }
}