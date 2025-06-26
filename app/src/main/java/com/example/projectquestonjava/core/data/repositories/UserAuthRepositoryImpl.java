package com.example.projectquestonjava.core.data.repositories;

import androidx.annotation.NonNull;
import com.example.projectquestonjava.core.data.dao.UserAuthDao;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserAuthRepositoryImpl implements UserAuthRepository {

    private static final String TAG = "UserAuthRepositoryImpl";
    private final UserAuthDao userAuthDao;
    private final Executor ioExecutor; // Вместо CoroutineDispatcher
    private final Logger logger;

    @Inject
    public UserAuthRepositoryImpl(
            UserAuthDao userAuthDao,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.userAuthDao = userAuthDao;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    @Override
    public ListenableFuture<UserAuth> getUserById(int userId) {
        logger.debug(TAG, "Getting user by id=" + userId);
        return Futures.catchingAsync(
                userAuthDao.getUserById(userId),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting user by id=" + userId, e);
                    return null;
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> updateUsername(int userId, String newName) {
        logger.debug(TAG, "Updating username for userId=" + userId + " to '" + newName + "'");
        ListenableFuture<Integer> updateFuture = userAuthDao.updateUsername(userId, newName);
        return Futures.transformAsync(updateFuture, updatedRows -> {
            if (updatedRows == 0) {
                String msg = "Username update failed: User with ID " + userId + " not found.";
                logger.warn(TAG, msg);
                return Futures.immediateFailedFuture(new NoSuchElementException(msg));
            }
            logger.info(TAG, "Username updated successfully for userId=" + userId);
            return Futures.immediateFuture(null);
        }, ioExecutor);
    }

    @Override
    public ListenableFuture<Void> updateUser(UserAuth user) {
        logger.debug(TAG, "Updating user with id=" + user.getId());
        // userAuthDao.updateUser возвращает ListenableFuture<Integer>
        ListenableFuture<Integer> updateFuture = userAuthDao.updateUser(user);
        return Futures.transformAsync(updateFuture, updatedRows -> {
            // Можно добавить проверку updatedRows, если DAO возвращает количество
            logger.info(TAG, "User " + user.getId() + " updated successfully.");
            return Futures.immediateFuture(null);
        }, ioExecutor);
    }

    @Override
    public ListenableFuture<Void> updateAvatarUrl(int userId, String avatarPath) {
        logger.debug(TAG, "Updating avatarUrl for userId=" + userId + " to '" + avatarPath + "'");
        ListenableFuture<Integer> updateFuture = userAuthDao.updateAvatarUrl(userId, avatarPath);
        return Futures.transformAsync(updateFuture, updatedRows -> {
            if (updatedRows == 0) {
                String msg = "Avatar URL update failed: User with ID " + userId + " not found.";
                logger.warn(TAG, msg);
                return Futures.immediateFailedFuture(new NoSuchElementException(msg));
            }
            logger.info(TAG, "Avatar URL updated successfully for userId=" + userId);
            return Futures.immediateFuture(null);
        }, ioExecutor);
    }

    @Override
    public ListenableFuture<Void> updatePasswordHash(int userId, String newHash) {
        logger.debug(TAG, "Updating password hash for userId=" + userId);
        ListenableFuture<Integer> updateFuture = userAuthDao.updatePasswordHash(userId, newHash);
        return Futures.transformAsync(updateFuture, updatedRows -> {
            if (updatedRows == 0) {
                String msg = "Password hash update failed: User with ID " + userId + " not found.";
                logger.warn(TAG, msg);
                return Futures.immediateFailedFuture(new NoSuchElementException(msg));
            }
            logger.info(TAG, "Password hash updated successfully for userId=" + userId);
            return Futures.immediateFuture(null);
        }, ioExecutor);
    }
}