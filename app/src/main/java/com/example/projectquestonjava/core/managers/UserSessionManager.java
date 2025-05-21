package com.example.projectquestonjava.core.managers;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys; // Вместо preferences.core.intPreferencesKey
import androidx.lifecycle.LiveData; // Для Flow -> LiveData
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.utils.Logger;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class UserSessionManager {

    public static final int NO_USER_ID = -1;
    private static final Preferences.Key<Integer> USER_ID_KEY = PreferencesKeys.intKey("user_id");
    private final DataStoreManager dataStoreManager;
    private final Logger logger;

    @Inject
    public UserSessionManager(DataStoreManager dataStoreManager, Logger logger) {
        this.dataStoreManager = dataStoreManager;
        this.logger = logger;
    }

    // Вместо Flow<Integer> используем LiveData<Integer>
    public LiveData<Integer> getUserIdLiveData() {
        return dataStoreManager.getFlow(USER_ID_KEY, NO_USER_ID);
        // DataStoreManager.getFlow должен быть адаптирован для возврата LiveData,
        // либо здесь мы его трансформируем. Пока оставим так, DataStoreManager будет переписан позже.
    }

    // Синхронное получение ID (стараться избегать, если DataStoreManager асинхронный)
    // Потребует адаптации DataStoreManager
    public int getUserIdSync() {
        try {
            // Это блокирующая операция, если DataStoreManager.getValue асинхронен
            return dataStoreManager.getValue(USER_ID_KEY, NO_USER_ID);
        } catch (IOException e) {
            logger.error("UserSessionManager", "Error getting user ID synchronously", e);
            return NO_USER_ID;
        }
    }


    public void saveUserId(int newUserId) {
        dataStoreManager.saveValue(USER_ID_KEY, newUserId, result -> {
            if (result.isFailure()) {
                logger.error("UserSessionManager", "Failed to save user ID", result.exceptionOrNull());
            }
        });
    }

    public void clearUserId() {
        dataStoreManager.clearValue(USER_ID_KEY, result -> {
            if (result.isFailure()) {
                logger.error("UserSessionManager", "Failed to clear user ID", result.exceptionOrNull());
            }
        });
    }
}