package com.example.projectquestonjava.core.managers;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.utils.Logger; // Добавил Logger
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkspaceSessionManager {

    private static final Preferences.Key<Long> WORKSPACE_ID_KEY = PreferencesKeys.longKey("workspace_id");
    private final DataStoreManager dataStoreManager;
    private final Logger logger; // Добавил Logger

    @Inject
    public WorkspaceSessionManager(DataStoreManager dataStoreManager, Logger logger) {
        this.dataStoreManager = dataStoreManager;
        this.logger = logger; // Инициализировал Logger
    }

    public LiveData<Long> getWorkspaceIdLiveData() {
        return dataStoreManager.getFlow(WORKSPACE_ID_KEY, 0L);
    }

    // Синхронное получение, если очень нужно
    public long getWorkspaceIdSync() {
        try {
            return dataStoreManager.getValue(WORKSPACE_ID_KEY, 0L);
        } catch (IOException e) {
            logger.error("WorkspaceSessionManager", "Error getting workspace ID synchronously", e);
            return 0L;
        }
    }


    public void saveWorkspaceId(long newWorkspaceId) {
        dataStoreManager.saveValue(WORKSPACE_ID_KEY, newWorkspaceId, result -> {
            if (result.isFailure()) {
                logger.error("WorkspaceSessionManager", "Failed to save workspace ID", result.exceptionOrNull());
            }
        });
    }

    public void clearWorkspaceId() {
        dataStoreManager.clearValue(WORKSPACE_ID_KEY, result -> {
            if (result.isFailure()) {
                logger.error("WorkspaceSessionManager", "Failed to clear workspace ID", result.exceptionOrNull());
            }
        });
    }
}