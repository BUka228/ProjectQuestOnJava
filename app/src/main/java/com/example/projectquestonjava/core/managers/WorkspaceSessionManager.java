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
import java.io.IOException;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkspaceSessionManager {

    private static final Preferences.Key<Long> WORKSPACE_ID_KEY = PreferencesKeys.longKey("workspace_id");
    private final DataStoreManager dataStoreManager;
    private final Logger logger;
    private final Executor directExecutor;

    @Inject
    public WorkspaceSessionManager(DataStoreManager dataStoreManager, Logger logger) {
        this.dataStoreManager = dataStoreManager;
        this.logger = logger;
        this.directExecutor = MoreExecutors.directExecutor();
    }

    public LiveData<Long> getWorkspaceIdLiveData() {
        return dataStoreManager.getPreferenceLiveData(WORKSPACE_ID_KEY, 0L);
    }

    public long getWorkspaceIdSync() {
        try {
            return dataStoreManager.getValueFuture(WORKSPACE_ID_KEY, 0L).get();
        } catch (Exception e) {
            logger.error("WorkspaceSessionManager", "Error getting workspace ID synchronously", e);
            return 0L;
        }
    }

    public ListenableFuture<Void> saveWorkspaceIdAsync(long newWorkspaceId) {
        logger.debug("WorkspaceSessionManager", "Saving workspace ID: " + newWorkspaceId);
        return dataStoreManager.saveValueFuture(WORKSPACE_ID_KEY, newWorkspaceId);
    }

    public void saveWorkspaceId(long newWorkspaceId) {
        Futures.addCallback(saveWorkspaceIdAsync(newWorkspaceId), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.debug("WorkspaceSessionManager", "Workspace ID saved successfully: " + newWorkspaceId);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error("WorkspaceSessionManager", "Failed to save workspace ID: " + newWorkspaceId, t);
            }
        }, directExecutor);
    }

    public ListenableFuture<Void> clearWorkspaceIdAsync() {
        logger.debug("WorkspaceSessionManager", "Clearing workspace ID");
        return dataStoreManager.clearValueFuture(WORKSPACE_ID_KEY);
    }

    public void clearWorkspaceId() {
        Futures.addCallback(clearWorkspaceIdAsync(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.debug("WorkspaceSessionManager", "Workspace ID cleared successfully.");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error("WorkspaceSessionManager", "Failed to clear workspace ID", t);
            }
        }, directExecutor);
    }
}