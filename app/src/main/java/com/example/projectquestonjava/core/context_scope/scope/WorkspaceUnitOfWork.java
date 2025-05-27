package com.example.projectquestonjava.core.context_scope.scope;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable; // Для операций, возвращающих значение

/**
 * Расширяет UnitOfWork для выполнения операций в контексте текущего рабочего пространства,
 * опционально с транзакциями базы данных.
 */
public interface WorkspaceUnitOfWork extends UnitOfWork {

    /**
     * Выполняет операцию в контексте текущего рабочего пространства.
     * Операция может быть асинхронной и возвращать ListenableFuture.
     * Транзакция базы данных НЕ используется.
     *
     * @param operation Функция, принимающая workspaceId и возвращающая ListenableFuture<T>.
     * @param <T> Тип результата операции.
     * @return ListenableFuture<T>, представляющий результат асинхронной операции.
     */
    <T> ListenableFuture<T> executeInWorkspaceAsync(WorkspaceIdAsyncOperation<T> operation);

    /**
     * Выполняет операцию в контексте текущего рабочего пространства.
     * Операция может быть синхронной (блокирующей) и будет выполнена на фоновом потоке.
     * Транзакция базы данных НЕ используется.
     *
     * @param operation Функция, принимающая workspaceId и возвращающая результат T.
     * @param <T> Тип результата операции.
     * @return ListenableFuture<T>, представляющий результат операции.
     */
    <T> ListenableFuture<T> executeInWorkspace(WorkspaceIdOperation<T> operation);


    /**
     * Выполняет операцию в контексте текущего рабочего пространства ВНУТРИ транзакции базы данных.
     * Операция может быть асинхронной (возвращать ListenableFuture).
     * Вся операция (получение workspaceId и выполнение `operation`) будет обернута в транзакцию.
     *
     * @param operation Функция, принимающая workspaceId и возвращающая ListenableFuture<T>.
     * @param <T> Тип результата операции.
     * @return ListenableFuture<T>, представляющий результат асинхронной операции.
     */
    <T> ListenableFuture<T> executeInWorkspaceWithTransactionAsync(WorkspaceIdAsyncOperation<T> operation);


    /**
     * Выполняет операцию в контексте текущего рабочего пространства ВНУТРИ транзакции базы данных.
     * Операция может быть синхронной (блокирующей) и будет выполнена на фоновом потоке.
     * Вся операция (получение workspaceId и выполнение `operation`) будет обернута в транзакцию.
     *
     * @param operation Функция, принимающая workspaceId и возвращающая результат T.
     * @param <T> Тип результата операции.
     * @return ListenableFuture<T>, представляющий результат операции.
     */
    <T> ListenableFuture<T> executeInWorkspaceWithTransaction(WorkspaceIdOperation<T> operation);


    // Функциональные интерфейсы для лямбд
    @FunctionalInterface
    interface WorkspaceIdAsyncOperation<T> {
        ListenableFuture<T> apply(long workspaceId) throws Exception; // Может бросать Exception
    }

    @FunctionalInterface
    interface WorkspaceIdOperation<T> {
        T apply(long workspaceId) throws Exception; // Может бросать Exception
    }
}