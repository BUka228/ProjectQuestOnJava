package com.example.projectquestonjava.feature.profile.domain.usecases;

import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.core.domain.security.PasswordHasher;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class UpdatePasswordUseCase {
    private static final String TAG = "UpdatePasswordUseCase";

    private final UserAuthRepository userAuthRepository;
    private final UserSessionManager userSessionManager;
    private final PasswordHasher passwordHasher;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public UpdatePasswordUseCase(
            UserAuthRepository userAuthRepository,
            UserSessionManager userSessionManager,
            PasswordHasher passwordHasher,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.userAuthRepository = userAuthRepository;
        this.userSessionManager = userSessionManager;
        this.passwordHasher = passwordHasher;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> execute(String currentPassword, String newPassword) {
        return Futures.submitAsync(() -> {
            if (newPassword == null || newPassword.length() < 8) {
                logger.warn(TAG, "Validation failed: New password too short or null.");
                return Futures.immediateFailedFuture(new IllegalArgumentException("Новый пароль должен быть не менее 8 символов."));
            }

            int userId = userSessionManager.getUserIdSync();
            if (userId == UserSessionManager.NO_USER_ID) {
                logger.error(TAG, "Cannot update password: User not logged in.");
                return Futures.immediateFailedFuture(new IllegalStateException("Пользователь не авторизован."));
            }
            logger.debug(TAG, "Attempting to update password for userId=" + userId);

            try {
                UserAuth currentUser = Futures.getDone(userAuthRepository.getUserById(userId));
                if (currentUser == null) {
                    throw new NoSuchElementException("User not found for password update.");
                }

                if (!passwordHasher.verify(currentPassword, currentUser.getPasswordHash())) {
                    logger.warn(TAG, "Password update failed: Incorrect current password for userId=" + userId);
                    return Futures.immediateFailedFuture(new IllegalArgumentException("Неверный текущий пароль."));
                }

                String newPasswordHash = passwordHasher.hash(newPassword);
                Futures.getDone(userAuthRepository.updatePasswordHash(userId, newPasswordHash));

                logger.info(TAG, "Password updated successfully for userId=" + userId);
                return Futures.immediateFuture(null);
            } catch (Exception e) {
                logger.error(TAG, "Failed to update password for userId=" + userId, e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }
}