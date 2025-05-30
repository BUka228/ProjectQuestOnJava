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
import com.google.common.util.concurrent.MoreExecutors; // Для directExecutor, если нужен

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
        final int userId = userSessionManager.getUserIdSync(); // Получаем userId синхронно, т.к. сам use case будет на ioExecutor
        if (userId == UserSessionManager.NO_USER_ID) {
            logger.error(TAG, "Cannot update avatar: User not logged in.");
            return Futures.immediateFailedFuture(new IllegalStateException("Пользователь не авторизован."));
        }
        logger.debug(TAG, "Attempting to update avatar for userId=" + userId + " from URI: " + newAvatarUri);

        // 1. Получаем текущего пользователя (и его старый путь к аватару)
        ListenableFuture<UserAuth> userFuture = userAuthRepository.getUserById(userId);

        // 2. Сохраняем новый аватар (эта операция может быть долгой)
        ListenableFuture<String> newAvatarPathFuture = avatarStorageHelper.saveAvatar(userId, newAvatarUri);

        // 3. Комбинируем результаты и выполняем последующие действия
        return Futures.whenAllSucceed(userFuture, newAvatarPathFuture)
                .callAsync(() -> {
                    UserAuth currentUser = Futures.getDone(userFuture); // Теперь безопасно, т.к. whenAllSucceed дождался
                    String newAvatarPath = Futures.getDone(newAvatarPathFuture); // Тоже безопасно
                    String oldAvatarPath = (currentUser != null) ? currentUser.getAvatarUrl() : null;

                    logger.debug(TAG, "User loaded, new avatar saved. Old path: " + oldAvatarPath + ", New path: " + newAvatarPath);

                    // 3.1 Обновляем путь в репозитории UserAuthRepository
                    ListenableFuture<Void> updateRepoFuture = userAuthRepository.updateAvatarUrl(userId, newAvatarPath);

                    return Futures.transformAsync(updateRepoFuture, aVoid -> {
                        logger.info(TAG, "Avatar path updated in repository for userId=" + userId);
                        // 3.2 Удаляем старый файл, если он был и отличается от нового
                        if (oldAvatarPath != null && !oldAvatarPath.trim().isEmpty() && !Objects.equals(oldAvatarPath, newAvatarPath)) {
                            // avatarStorageHelper.deleteAvatar возвращает ListenableFuture<Void>,
                            // мы можем его дождаться или запустить и забыть (если удаление не критично)
                            // Для последовательности, дождемся.
                            return Futures.transform(avatarStorageHelper.deleteAvatar(oldAvatarPath), input -> newAvatarPath, ioExecutor);
                        } else {
                            return Futures.immediateFuture(newAvatarPath);
                        }
                    }, ioExecutor); // transformAsync для updateRepoFuture
                }, ioExecutor); // callAsync для whenAllSucceed
    }
}