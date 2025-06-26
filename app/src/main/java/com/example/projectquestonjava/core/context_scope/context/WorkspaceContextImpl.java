package com.example.projectquestonjava.core.context_scope.context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher; // Ваша аннотация для Executor
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkspaceContextImpl implements WorkspaceContext {

    private static final String TAG = "WorkspaceContextImpl";
    private final WorkspaceSessionManager workspaceSessionManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public WorkspaceContextImpl(
            WorkspaceSessionManager workspaceSessionManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.workspaceSessionManager = workspaceSessionManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    @Override
    public <T> LiveData<T> executeWithWorkspaceLiveData(WorkspaceIdFunctionLiveData<T> operation) {
        // Используем Transformations.switchMap для реакции на изменение workspaceId
        return Transformations.switchMap(workspaceSessionManager.getWorkspaceIdLiveData(), workspaceId -> {
            if (workspaceId == null || workspaceId == 0L) {
                logger.warn(TAG, "executeWithWorkspaceLiveData: No valid workspaceId (" + workspaceId + "). Returning empty/default LiveData.");
                // Возвращаем LiveData с null или дефолтным значением, если операция должна что-то вернуть
                return new LiveData<T>(null) {}; // Пример пустого LiveData
            }
            logger.debug(TAG, "executeWithWorkspaceLiveData: Switching context to workspaceId: " + workspaceId);
            try {
                return operation.apply(workspaceId);
            } catch (Exception e) {
                logger.error(TAG, "executeWithWorkspaceLiveData: Error in operation for workspaceId: " + workspaceId, e);
                // Можно вернуть LiveData с ошибкой или пустое
                // errorLiveData.setValue( /* можно установить специальное значение ошибки */ );
                return new MutableLiveData<T>();
            }
        });
    }

    @Override
    public <T> ListenableFuture<T> executeWithCurrentWorkspaceFuture(WorkspaceIdFunctionFuture<T> operation) {
        return Futures.transformAsync(
                workspaceSessionManager.getWorkspaceIdFuture(), // Получаем ListenableFuture<Long>
                workspaceId -> {
                    if (workspaceId == null || workspaceId == 0L) {
                        logger.warn(TAG, "executeWithCurrentWorkspaceFuture: No valid workspaceId (" + workspaceId + "). Failing operation.");
                        return Futures.immediateFailedFuture(new IllegalStateException("No active workspace set"));
                    }
                    logger.debug(TAG, "executeWithCurrentWorkspaceFuture: Executing with workspaceId: " + workspaceId);
                    try {
                        return operation.apply(workspaceId);
                    } catch (Exception e) {
                        logger.error(TAG, "executeWithCurrentWorkspaceFuture: Error in operation for workspaceId: " + workspaceId, e);
                        return Futures.immediateFailedFuture(e);
                    }
                },
                ioExecutor // Executor для выполнения transformAsync
        );
    }

    @Override
    public <T> ListenableFuture<T> executeWithCurrentWorkspaceCallable(WorkspaceIdCallable<T> operation) {
        return Futures.submitAsync(() -> { // Выполняем на ioExecutor
            try {
                long workspaceId = workspaceSessionManager.getWorkspaceIdSync(); // Блокирующий вызов, но мы на ioExecutor
                if (workspaceId == 0L) {
                    logger.warn(TAG, "executeWithCurrentWorkspaceCallable: No valid workspaceId. Failing operation.");
                    throw new IllegalStateException("No active workspace set");
                }
                logger.debug(TAG, "executeWithCurrentWorkspaceCallable: Executing with workspaceId: " + workspaceId);
                return Futures.immediateFuture(operation.call(workspaceId));
            } catch (Exception e) {
                logger.error(TAG, "executeWithCurrentWorkspaceCallable: Error in operation", e);
                throw e; // Пробрасываем для ListenableFuture
            }
        }, ioExecutor);
    }


    @Override
    public LiveData<Long> getCurrentWorkspaceIdLiveData() {
        return workspaceSessionManager.getWorkspaceIdLiveData();
    }

    @Override
    public ListenableFuture<Long> getCurrentWorkspaceIdFuture() {
        return workspaceSessionManager.getWorkspaceIdFuture();
    }

    @Override
    public long getCurrentWorkspaceIdSync() {
        // Этот метод уже реализован в WorkspaceSessionManager
        return workspaceSessionManager.getWorkspaceIdSync();
    }
}