package com.example.projectquestonjava.core.context_scope.scope;

import com.example.projectquestonjava.core.context_scope.context.WorkspaceContext;
import com.example.projectquestonjava.core.di.IODispatcher; // Для Executor
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkspaceUnitOfWorkImpl implements WorkspaceUnitOfWork {

    private static final String TAG = "WorkspaceUnitOfWork";
    private final WorkspaceContext workspaceContext;
    private final UnitOfWork databaseUnitOfWork; // Базовый UnitOfWork для транзакций БД
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public WorkspaceUnitOfWorkImpl(
            WorkspaceContext workspaceContext,
            UnitOfWork databaseUnitOfWork, // Внедряем DatabaseUnitOfWork под интерфейсом UnitOfWork
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.workspaceContext = workspaceContext;
        this.databaseUnitOfWork = databaseUnitOfWork;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    // Методы из UnitOfWork (делегирование)
    @Override
    public <T> T withTransaction(Callable<T> block) throws Exception {
        return databaseUnitOfWork.withTransaction(block);
    }

    @Override
    public void withTransaction(Runnable block) throws Exception {
        databaseUnitOfWork.withTransaction(block);
    }


    // Методы из WorkspaceUnitOfWork

    @Override
    public <T> ListenableFuture<T> executeInWorkspaceAsync(WorkspaceIdAsyncOperation<T> operation) {
        logger.debug(TAG, "Executing ASYNC operation in workspace context (no transaction)...");
        // workspaceContext.executeWithCurrentWorkspaceFuture уже выполняет операцию на ioExecutor
        return workspaceContext.executeWithCurrentWorkspaceFuture(operation::apply);
    }

    @Override
    public <T> ListenableFuture<T> executeInWorkspace(WorkspaceIdOperation<T> operation) {
        logger.debug(TAG, "Executing SYNC operation in workspace context (no transaction)...");
        // workspaceContext.executeWithCurrentWorkspaceCallable уже выполняет операцию на ioExecutor
        return workspaceContext.executeWithCurrentWorkspaceCallable(operation::apply);
    }


    @Override
    public <T> ListenableFuture<T> executeInWorkspaceWithTransactionAsync(WorkspaceIdAsyncOperation<T> operation) {
        logger.debug(TAG, "Executing ASYNC operation in workspace context WITH TRANSACTION...");
        // Оборачиваем всю логику (включая получение workspaceId и выполнение operation) в submitAsync,
        // а затем внутри этого вызываем databaseUnitOfWork.withTransaction.
        // Сама operation тоже асинхронна (возвращает ListenableFuture).
        return Futures.submitAsync(() -> {
            try {
                // Получаем workspaceId синхронно, т.к. мы уже на ioExecutor
                long workspaceId = workspaceContext.getCurrentWorkspaceIdSync();
                if (workspaceId == 0L) {
                    throw new IllegalStateException("No active workspace set for transaction.");
                }
                // Выполняем операцию ВНУТРИ транзакции
                // И возвращаем результат ListenableFuture из withTransaction
                return databaseUnitOfWork.withTransaction(() -> {
                    logger.debug(TAG, "Transaction started for ASYNC operation in workspace: " + workspaceId);
                    return operation.apply(workspaceId); // Это вернет ListenableFuture<T>
                });
            } catch (Exception e) {
                logger.error(TAG, "Error during executeInWorkspaceWithTransactionAsync setup", e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }

    @Override
    public <T> ListenableFuture<T> executeInWorkspaceWithTransaction(WorkspaceIdOperation<T> operation) {
        logger.debug(TAG, "Executing SYNC operation in workspace context WITH TRANSACTION...");
        return Futures.submitAsync(() -> {
            try {
                long workspaceId = workspaceContext.getCurrentWorkspaceIdSync();
                if (workspaceId == 0L) {
                    throw new IllegalStateException("No active workspace set for transaction.");
                }
                T result = databaseUnitOfWork.withTransaction(() -> {
                    logger.debug(TAG, "Transaction started for SYNC operation in workspace: " + workspaceId);
                    return operation.apply(workspaceId); // Это вернет T
                });
                return Futures.immediateFuture(result);
            } catch (Exception e) {
                logger.error(TAG, "Error during executeInWorkspaceWithTransaction", e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }
}