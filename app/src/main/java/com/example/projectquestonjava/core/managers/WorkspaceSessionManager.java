package com.example.projectquestonjava.core.managers;

import androidx.annotation.NonNull;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors; // Для directExecutor
import java.io.IOException; // Для getWorkspaceIdSync
import java.util.concurrent.ExecutionException; // Для getWorkspaceIdSync
import java.util.concurrent.Executor; // Если нужен специфичный Executor для коллбеков
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkspaceSessionManager {

    private static final String TAG = "WorkspaceSessionManager"; // Добавил TAG для логгирования
    private static final Preferences.Key<Long> WORKSPACE_ID_KEY = PreferencesKeys.longKey("workspace_id");
    public static final long NO_WORKSPACE_ID = 0L; // Константа для отсутствующего ID (или -1L, если предпочитаешь)

    private final DataStoreManager dataStoreManager;
    private final Logger logger;
    private final Executor directExecutor; // Для простых коллбеков, выполняющихся в потоке Future

    @Inject
    public WorkspaceSessionManager(DataStoreManager dataStoreManager, Logger logger) {
        this.dataStoreManager = dataStoreManager;
        this.logger = logger;
        this.directExecutor = MoreExecutors.directExecutor();
    }

    /**
     * Возвращает LiveData, который эмитит текущий ID рабочего пространства.
     * Эмитит {@link #NO_WORKSPACE_ID} если ID не установлен.
     */
    public LiveData<Long> getWorkspaceIdLiveData() {
        return dataStoreManager.getPreferenceLiveData(WORKSPACE_ID_KEY, NO_WORKSPACE_ID);
    }

    /**
     * Асинхронно получает текущий ID рабочего пространства.
     * @return ListenableFuture с ID рабочего пространства или {@link #NO_WORKSPACE_ID}, если не установлен.
     */
    public ListenableFuture<Long> getWorkspaceIdFuture() {
        return dataStoreManager.getValueFuture(WORKSPACE_ID_KEY, NO_WORKSPACE_ID);
    }

    /**
     * Синхронно получает текущий ID рабочего пространства.
     * ВНИМАНИЕ: Этот метод блокирует текущий поток до получения значения.
     * Используйте с осторожностью, предпочтительно на фоновом потоке.
     * @return ID рабочего пространства или {@link #NO_WORKSPACE_ID}, если не установлен или произошла ошибка.
     */
    public long getWorkspaceIdSync() {
        try {
            // dataStoreManager.getValueFuture().get() блокирует поток
            return dataStoreManager.getValueFuture(WORKSPACE_ID_KEY, NO_WORKSPACE_ID).get();
        } catch (ExecutionException | InterruptedException e) {
            logger.error(TAG, "Error getting workspace ID synchronously", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
            }
            return NO_WORKSPACE_ID; // Возвращаем значение по умолчанию при ошибке
        }
    }

    /**
     * Асинхронно сохраняет ID нового рабочего пространства.
     * @param newWorkspaceId ID нового рабочего пространства.
     * @return ListenableFuture<Void> для отслеживания завершения операции.
     */
    public ListenableFuture<Void> saveWorkspaceIdAsync(long newWorkspaceId) {
        logger.debug(TAG, "Saving workspace ID: " + newWorkspaceId);
        return dataStoreManager.saveValueFuture(WORKSPACE_ID_KEY, newWorkspaceId);
    }

    /**
     * Сохраняет ID нового рабочего пространства (fire-and-forget с логированием).
     * @param newWorkspaceId ID нового рабочего пространства.
     */
    public void saveWorkspaceId(long newWorkspaceId) {
        Futures.addCallback(saveWorkspaceIdAsync(newWorkspaceId), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Workspace ID saved successfully: " + newWorkspaceId);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to save workspace ID: " + newWorkspaceId, t);
            }
        }, directExecutor);
    }

    /**
     * Асинхронно очищает сохраненный ID рабочего пространства.
     * @return ListenableFuture<Void> для отслеживания завершения операции.
     */
    public ListenableFuture<Void> clearWorkspaceIdAsync() {
        logger.debug(TAG, "Clearing workspace ID");
        return dataStoreManager.clearValueFuture(WORKSPACE_ID_KEY);
    }

    /**
     * Очищает сохраненный ID рабочего пространства (fire-and-forget с логированием).
     */
    public void clearWorkspaceId() {
        Futures.addCallback(clearWorkspaceIdAsync(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Workspace ID cleared successfully.");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to clear workspace ID", t);
            }
        }, directExecutor);
    }
}