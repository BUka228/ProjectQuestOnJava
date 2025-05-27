package com.example.projectquestonjava.core.context_scope.context;

import androidx.lifecycle.LiveData;
import com.google.common.util.concurrent.ListenableFuture;

public interface WorkspaceContext {

    /**
     * Выполняет операцию, которая возвращает LiveData<T>, в контексте текущего workspaceId.
     * Как только workspaceId меняется, LiveData пересоздается.
     *
     * @param operation Функция, принимающая workspaceId и возвращающая LiveData<T>.
     * @param <T> Тип данных в LiveData.
     * @return LiveData<T>, который будет обновляться при смене workspaceId.
     */
    <T> LiveData<T> executeWithWorkspaceLiveData(WorkspaceIdFunctionLiveData<T> operation);

    /**
     * Выполняет однократную асинхронную операцию, которая возвращает ListenableFuture<T>,
     * в контексте текущего workspaceId.
     *
     * @param operation Функция, принимающая workspaceId и возвращающая ListenableFuture<T>.
     * @param <T> Тип результата операции.
     * @return ListenableFuture<T>, представляющий результат асинхронной операции.
     */
    <T> ListenableFuture<T> executeWithCurrentWorkspaceFuture(WorkspaceIdFunctionFuture<T> operation);

    /**
     * Выполняет однократную синхронную операцию (но внутри ListenableFuture для асинхронности)
     * в контексте текущего workspaceId.
     * Предполагается, что сама `operation` может быть блокирующей и будет выполнена на фоновом потоке.
     *
     * @param operation Функция, принимающая workspaceId и возвращающая результат типа T.
     * @param <T> Тип результата операции.
     * @return ListenableFuture<T>, представляющий результат операции.
     */
    <T> ListenableFuture<T> executeWithCurrentWorkspaceCallable(WorkspaceIdCallable<T> operation);


    /**
     * Возвращает LiveData текущего workspaceId.
     */
    LiveData<Long> getCurrentWorkspaceIdLiveData();

    /**
     * Возвращает ListenableFuture для получения текущего workspaceId.
     * Полезно, когда нужно получить ID один раз асинхронно.
     */
    ListenableFuture<Long> getCurrentWorkspaceIdFuture();

    /**
     * Возвращает текущий workspaceId синхронно.
     * ВНИМАНИЕ: Этот метод может блокировать поток, если значение еще не загружено.
     * Используйте с осторожностью и только если уверены, что значение доступно или вы уже в фоновом потоке.
     */
    long getCurrentWorkspaceIdSync();

    // Функциональные интерфейсы для передачи лямбд
    @FunctionalInterface
    interface WorkspaceIdFunctionLiveData<T> {
        LiveData<T> apply(long workspaceId);
    }

    @FunctionalInterface
    interface WorkspaceIdFunctionFuture<T> {
        ListenableFuture<T> apply(long workspaceId) throws Exception;
    }

    @FunctionalInterface
    interface WorkspaceIdCallable<T> {
        T call(long workspaceId) throws Exception; // Callable может бросать Exception
    }
}