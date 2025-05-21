package com.example.projectquestonjava.feature.profile.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import javax.inject.Inject;

public class UpdateUsernameUseCase {
    private static final String TAG = "UpdateUsernameUseCase";

    private final UserAuthRepository userAuthRepository;
    private final UserSessionManager userSessionManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public UpdateUsernameUseCase(
            UserAuthRepository userAuthRepository,
            UserSessionManager userSessionManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.userAuthRepository = userAuthRepository;
        this.userSessionManager = userSessionManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(String newName) {
        return Futures.submitAsync(() -> {
            String trimmedName = (newName != null) ? newName.trim() : "";
            if (trimmedName.isEmpty()) {
                logger.warn(TAG, "Validation failed: Username cannot be blank.");
                return Futures.immediateFailedFuture(new IllegalArgumentException("Имя пользователя не может быть пустым."));
            }
            if (trimmedName.length() > 50) {
                logger.warn(TAG, "Validation failed: Username too long (length: " + trimmedName.length() + ")");
                return Futures.immediateFailedFuture(new IllegalArgumentException("Имя пользователя слишком длинное (макс. 50 символов)."));
            }

            int userId = userSessionManager.getUserIdSync();
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.error(TAG, "Cannot update username: User not logged in.");
                return Futures.immediateFailedFuture(new IllegalStateException("Пользователь не авторизован."));
            }
            logger.debug(TAG, "Attempting to update username for userId=" + userId + " to '" + trimmedName + "'");

            return Futures.catchingAsync(
                    userAuthRepository.updateUsername(userId, trimmedName), // Это уже ListenableFuture<Void>
                    Exception.class,
                    e -> {
                        logger.error(TAG, "Username update failed for userId=" + userId, e);
                        throw new RuntimeException("Failed to update username", e);
                    },
                    ioExecutor
            );
        }, ioExecutor);
    }
}