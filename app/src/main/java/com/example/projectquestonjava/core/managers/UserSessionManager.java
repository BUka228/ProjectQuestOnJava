package com.example.projectquestonjava.core.managers;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;

@Singleton
public class UserSessionManager {

    public static final int NO_USER_ID = -1;
    private static final Preferences.Key<Integer> USER_ID_KEY = PreferencesKeys.intKey("user_id");

    private final DataStoreManager dataStoreManager;
    private final UserAuthRepository userAuthRepository; // Добавляем зависимость
    private final Executor ioExecutor; // Для выполнения операций репозитория
    private final Logger logger;

    @Inject
    public UserSessionManager(
            DataStoreManager dataStoreManager,
            UserAuthRepository userAuthRepository, // Внедряем репозиторий
            @com.example.projectquestonjava.core.di.IODispatcher Executor ioExecutor, // Убедись, что аннотация правильная
            Logger logger) {
        this.dataStoreManager = dataStoreManager;
        this.userAuthRepository = userAuthRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public LiveData<Integer> getUserIdLiveData() {
        return dataStoreManager.getPreferenceLiveData(USER_ID_KEY, NO_USER_ID);
    }

    public int getUserIdSync() {
        try {
            return dataStoreManager.getValueFuture(USER_ID_KEY, NO_USER_ID).get();
        } catch (Exception e) {
            logger.error("UserSessionManager", "Error getting user ID synchronously", e);
            return NO_USER_ID;
        }
    }

    public ListenableFuture<Integer> getUserIdFuture() {
        return dataStoreManager.getValueFuture(USER_ID_KEY, NO_USER_ID);
    }

    /**
     * Асинхронно загружает данные текущего авторизованного пользователя.
     *
     * @return ListenableFuture, который вернет UserAuth или null, если пользователь не найден или не авторизован.
     *         Future может завершиться с ошибкой, если произойдет сбой при доступе к данным.
     */
    public ListenableFuture<UserAuth> getCurrentUser() {
        logger.debug("UserSessionManager", "Attempting to get current user data...");
        // 1. Получаем userId асинхронно
        ListenableFuture<Integer> userIdFuture = getUserIdFuture();

        // 2. Когда userId получен, загружаем UserAuth
        return Futures.transformAsync(userIdFuture, userId -> {
            if (userId == null || userId == NO_USER_ID) {
                logger.warn("UserSessionManager", "No user ID found in session, cannot get current user.");
                return Futures.immediateFuture(null); // Пользователь не авторизован
            }
            // userAuthRepository.getUserById(userId) должен возвращать ListenableFuture<UserAuth>
            // (или ListenableFuture<Optional<UserAuth>> если используется Java 8 Optional)
            logger.debug("UserSessionManager", "Fetching user data for ID: " + userId);
            return userAuthRepository.getUserById(userId);
        }, ioExecutor); // Выполняем transformAsync на ioExecutor
    }


    public ListenableFuture<Void> saveUserIdAsync(int newUserId) {
        logger.debug("UserSessionManager", "Saving user ID async: " + newUserId);
        return dataStoreManager.saveValueFuture(USER_ID_KEY, newUserId);
    }

    public void saveUserId(int newUserId) {
        Futures.addCallback(saveUserIdAsync(newUserId), new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                logger.debug("UserSessionManager", "User ID saved successfully: " + newUserId);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error("UserSessionManager", "Failed to save user ID: " + newUserId, t);
            }
        }, MoreExecutors.directExecutor()); // Коллбэк можно выполнить в том же потоке, что и Future
    }

    public ListenableFuture<Void> clearUserIdAsync() {
        logger.debug("UserSessionManager", "Clearing user ID async");
        return dataStoreManager.clearValueFuture(USER_ID_KEY);
    }

    public void clearUserId() {
        Futures.addCallback(clearUserIdAsync(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.debug("UserSessionManager", "User ID cleared successfully.");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error("UserSessionManager", "Failed to clear user ID", t);
            }
        }, MoreExecutors.directExecutor());
    }
}