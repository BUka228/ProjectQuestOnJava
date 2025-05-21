package com.example.projectquestonjava.feature.profile.domain.usecases;

import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.AvatarStorageHelper;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

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
        return Futures.submitAsync(() -> {
            int userId = userSessionManager.getUserIdSync(); // Блокирующий вызов на ioExecutor
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.error(TAG, "Cannot remove avatar: User not logged in.");
                return Futures.immediateFailedFuture(new IllegalStateException("Пользователь не авторизован."));
            }
            logger.debug(TAG, "Attempting to remove avatar for userId=" + userId);

            try {
                // 1. Получаем текущего пользователя асинхронно
                ListenableFuture<UserAuth> userFuture = userAuthRepository.getUserById(userId);
                UserAuth currentUser = Futures.getDone(userFuture); // Блокируем, ожидая результат
                String currentAvatarPath = (currentUser != null) ? currentUser.getAvatarUrl() : null;

                if (currentAvatarPath == null || currentAvatarPath.trim().isEmpty()) {
                    logger.debug(TAG, "Avatar is already null for userId=" + userId + ". No action needed.");
                    return Futures.immediateFuture(null);
                }

                // 2. Обновляем путь в репозитории на null
                Futures.getDone(userAuthRepository.updateAvatarUrl(userId, null));

                // 3. Удаляем старый файл (AvatarStorageHelper.deleteAvatar теперь тоже ListenableFuture)
                Futures.getDone(avatarStorageHelper.deleteAvatar(currentAvatarPath));
                // Ошибки удаления файла логируются в хелпере, здесь не считаем критической

                logger.info(TAG, "Avatar successfully removed for userId=" + userId + ".");
                return Futures.immediateFuture(null);
            } catch (Exception e) {
                logger.error(TAG, "Failed to remove avatar for userId=" + userId, e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }
}