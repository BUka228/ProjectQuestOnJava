package com.example.projectquestonjava.core.managers;

import androidx.annotation.NonNull;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserSessionManager {

    public static final int NO_USER_ID = -1;
    private static final Preferences.Key<Integer> USER_ID_KEY = PreferencesKeys.intKey("user_id");
    private final DataStoreManager dataStoreManager;
    private final Logger logger;
    private final Executor directExecutor; // Можно использовать MoreExecutors.directExecutor()

    @Inject
    public UserSessionManager(DataStoreManager dataStoreManager, Logger logger) {
        this.dataStoreManager = dataStoreManager;
        this.logger = logger;
        this.directExecutor = MoreExecutors.directExecutor(); // Или внедрить свой Executor
    }

    public LiveData<Integer> getUserIdLiveData() {
        // DataStoreManager.getPreferenceLiveData теперь возвращает LiveData<T>
        return dataStoreManager.getPreferenceLiveData(USER_ID_KEY, NO_USER_ID);
    }

    // Синхронное получение ID (используем с осторожностью!)
    public int getUserIdSync() {
        try {
            // DataStoreManager.getValue теперь suspend, но для ListenableFuture есть обертка
            return dataStoreManager.getValueFuture(USER_ID_KEY, NO_USER_ID).get(); // Блокирующий вызов!
        } catch (Exception e) { // InterruptedException, ExecutionException
            logger.error("UserSessionManager", "Error getting user ID synchronously", e);
            return NO_USER_ID;
        }
    }

    public ListenableFuture<Void> saveUserIdAsync(int newUserId) {
        logger.debug("UserSessionManager", "Saving user ID: " + newUserId);
        // DataStoreManager.saveValueFuture теперь специфичен по типу
        return dataStoreManager.saveValueFuture(USER_ID_KEY, newUserId);
    }

    // Если нужен неблокирующий метод без возврата Future (fire-and-forget с логгированием)
    public void saveUserId(int newUserId) {
        Futures.addCallback(saveUserIdAsync(newUserId), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.debug("UserSessionManager", "User ID saved successfully: " + newUserId);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error("UserSessionManager", "Failed to save user ID: " + newUserId, t);
            }
        }, directExecutor);
    }


    public ListenableFuture<Void> clearUserIdAsync() {
        logger.debug("UserSessionManager", "Clearing user ID");
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
        }, directExecutor);
    }
}