package com.example.projectquestonjava.feature.profile.domain.usecases;

import android.net.Uri;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.AvatarStorageHelper;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class UpdateAvatarUseCase {
    private static final String TAG = "UpdateAvatarUseCase";

    private final UserAuthRepository userAuthRepository;
    private final UserSessionManager userSessionManager;
    private final AvatarStorageHelper avatarStorageHelper;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public UpdateAvatarUseCase(
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

    public ListenableFuture<String> execute(Uri newAvatarUri) {
        return Futures.submitAsync(() -> {
            int userId = userSessionManager.getUserIdSync();
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.error(TAG, "Cannot update avatar: User not logged in.");
                return Futures.immediateFailedFuture(new IllegalStateException("Пользователь не авторизован."));
            }
            logger.debug(TAG, "Attempting to update avatar for userId=" + userId + " from URI: " + newAvatarUri);

            try {
                // 1. Получаем текущий путь аватара
                UserAuth currentUser = Futures.getDone(userAuthRepository.getUserById(userId));
                String oldAvatarPath = (currentUser != null) ? currentUser.getAvatarUrl() : null;

                // 2. Сохраняем новый аватар
                String newAvatarPath = Futures.getDone(avatarStorageHelper.saveAvatar(userId, newAvatarUri));

                // 3. Обновляем путь в репозитории
                Futures.getDone(userAuthRepository.updateAvatarUrl(userId, newAvatarPath));

                // 4. Удаляем старый файл
                if (oldAvatarPath != null && !oldAvatarPath.trim().isEmpty() && !Objects.equals(oldAvatarPath, newAvatarPath)) {
                    Futures.getDone(avatarStorageHelper.deleteAvatar(oldAvatarPath));
                }

                logger.info(TAG, "Avatar successfully updated for userId=" + userId + ". New path: " + newAvatarPath);
                return Futures.immediateFuture(newAvatarPath);
            } catch (Exception e) {
                logger.error(TAG, "Failed to update avatar for userId=" + userId, e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }
}