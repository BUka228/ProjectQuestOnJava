// -------- A:\Progects\ProjectQuestOnJava\app\src\main\java\com\example\projectquestonjava\core\managers\DataStoreManager.java --------
package com.example.projectquestonjava.core.managers;

import android.content.Context; // Нужен для RxPreferenceDataStoreBuilder

import androidx.annotation.NonNull;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.rxjava3.RxPreferenceDataStoreBuilder;


import androidx.datastore.rxjava3.RxDataStore;
import androidx.datastore.rxjava3.RxDataStoreBuilder;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext; // Для внедрения Context
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler; // RxJava Scheduler
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public class DataStoreManager {
    private final RxDataStore<Preferences> rxDataStore;
    private final Logger logger;
    // ioExecutor больше не нужен здесь напрямую, т.к. Scheduler используется в RxPreferenceDataStoreBuilder

    @Inject
    public DataStoreManager(@ApplicationContext Context context, // Внедряем ApplicationContext
                            Logger logger,
                            @IODispatcher Executor ioExecutor) { // ioExecutor для Scheduler
        // Используем RxPreferenceDataStoreBuilder для создания RxDataStore
        this.rxDataStore = new RxDataStoreBuilder<>(context, "app_preferences_rx") // Имя файла для RxDataStore
                .setIoScheduler(Schedulers.from(ioExecutor)) // Устанавливаем Scheduler
                .build();
        this.logger = logger;
    }

    /**
     * Получает LiveData для значения из DataStore.
     * @param key Ключ Preferences.
     * @param defaultValue Значение по умолчанию.
     * @return LiveData<T>
     */
    public <T> LiveData<T> getFlow(Preferences.Key<T> key, T defaultValue) {
        Flowable<T> flowable = rxDataStore.data()
                .map(preferences -> {
                    T value = preferences.get(key);
                    return value != null ? value : defaultValue;
                })
                .onErrorReturn(throwable -> {
                    logger.error("DataStoreManager", "Error reading preferences for key: " + key.getName(), throwable);
                    return defaultValue;
                });
        return LiveDataReactiveStreams.fromPublisher(flowable);
    }

    /**
     * Асинхронно получает значение из DataStore, возвращая ListenableFuture.
     * @param key Ключ Preferences.
     * @param defaultValue Значение по умолчанию.
     * @return ListenableFuture<T>
     */
    public <T> ListenableFuture<T> getValueFuture(Preferences.Key<T> key, T defaultValue) {
        Single<T> single = rxDataStore.data().firstOrError()
                .map(preferences -> {
                    T value = preferences.get(key);
                    return value != null ? value : defaultValue;
                })
                .onErrorReturn(throwable -> {
                    logger.error("DataStoreManager", "Error getting value for key: " + key.getName(), throwable);
                    if (throwable instanceof IOException) {
                        throw (IOException) throwable;
                    }
                    throw new IOException("Failed to get value for key: " + key.getName(), throwable);
                });
        return JdkFutureAdapters.toListenableFuture(single);
    }


    /**
     * Синхронно получает значение из DataStore. Использовать с осторожностью.
     * @param key Ключ Preferences.
     * @param defaultValue Значение по умолчанию.
     * @return T значение.
     * @throws IOException если произошла ошибка чтения.
     */
    public <T> T getValue(Preferences.Key<T> key, T defaultValue) throws IOException {
        try {
            Preferences preferences = rxDataStore.data().blockingFirst();
            T value = preferences.get(key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            logger.error("DataStoreManager", "Error getting value synchronously for key: " + key.getName(), e);
            throw new IOException("Failed to get value synchronously for key: " + key.getName(), e);
        }
    }


    /**
     * Асинхронно сохраняет значение в DataStore.
     * @param key Ключ Preferences.
     * @param value Значение для сохранения.
     * @param callback FutureCallback для обработки результата.
     */
    public <T> void saveValue(@NonNull Preferences.Key<T> key, T value, @NonNull FutureCallback<Preferences> callback) {
        ListenableFuture<Preferences> future = JdkFutureAdapters.toListenableFuture(
                rxDataStore.updateDataAsync(prefsIn -> {
                    Preferences.MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
                    mutablePreferences.set(key, value);
                    logger.debug("DataStoreManager", "Value updated in DataStore: " + key.getName() + " -> " + value);
                    return Single.just(mutablePreferences.toPreferences());
                })
        );
        Futures.addCallback(future, callback, MoreExecutors.directExecutor());
    }

    /**
     * Асинхронно сохраняет значение в DataStore, возвращая ListenableFuture<Void>.
     * @param key Ключ Preferences.
     * @param value Значение для сохранения.
     * @return ListenableFuture<Void>
     */
    public <T> ListenableFuture<Void> saveValueFuture(@NonNull Preferences.Key<T> key, T value) {
        ListenableFuture<Preferences> updateFuture = JdkFutureAdapters.toListenableFuture(
                rxDataStore.updateDataAsync(prefsIn -> {
                    Preferences.MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
                    mutablePreferences.set(key, value);
                    logger.debug("DataStoreManager", "Value updated in DataStore: " + key.getName() + " -> " + value);
                    return Single.just(mutablePreferences.toPreferences());
                })
        );
        return Futures.transform(updateFuture, prefs -> null, MoreExecutors.directExecutor());
    }

    /**
     * Асинхронно удаляет значение из DataStore.
     * @param key Ключ Preferences для удаления.
     * @param callback FutureCallback для обработки результата.
     */
    public <T> void clearValue(@NonNull Preferences.Key<T> key, @NonNull FutureCallback<Preferences> callback) {
        ListenableFuture<Preferences> future = JdkFutureAdapters.toListenableFuture(
                rxDataStore.updateDataAsync(prefsIn -> {
                    Preferences.MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
                    mutablePreferences.remove(key);
                    logger.debug("DataStoreManager", "Value cleared in DataStore: " + key.getName());
                    return Single.just(mutablePreferences.toPreferences());
                })
        );
        Futures.addCallback(future, callback, MoreExecutors.directExecutor());
    }

    /**
     * Асинхронно удаляет значение из DataStore, возвращая ListenableFuture<Void>.
     * @param key Ключ Preferences для удаления.
     * @return ListenableFuture<Void>
     */
    public <T> ListenableFuture<Void> clearValueFuture(@NonNull Preferences.Key<T> key) {
        ListenableFuture<Preferences> updateFuture = JdkFutureAdapters.toListenableFuture(
                rxDataStore.updateDataAsync(prefsIn -> {
                    Preferences.MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
                    mutablePreferences.remove(key);
                    logger.debug("DataStoreManager", "Value cleared in DataStore: " + key.getName());
                    return Single.just(mutablePreferences.toPreferences());
                })
        );
        return Futures.transform(updateFuture, prefs -> null, MoreExecutors.directExecutor());
    }

    // Вспомогательный адаптер (упрощенный, для примера).
    private static class JdkFutureAdapters {
        public static <V> ListenableFuture<V> toListenableFuture(Single<V> single) {
            SettableListenableFuture<V> settableFuture = new SettableListenableFuture<>();
            // Важно: эта подписка должна быть обработана (dispose).
            // В DataStoreManager (Singleton) это менее критично, но для других случаев нужно управлять Disposable.
            // Здесь мы не храним Disposable, т.к. Single завершится либо успехом, либо ошибкой.
            single.subscribe(
                    settableFuture::set,
                    settableFuture::setException
            );
            return settableFuture;
        }
    }

    // Простая реализация ListenableFuture для адаптера.
    private static class SettableListenableFuture<V> extends com.google.common.util.concurrent.AbstractFuture<V> {
        @Override
        public boolean set(@androidx.annotation.Nullable V value) {
            return super.set(value);
        }

        @Override
        public boolean setException(@NonNull Throwable throwable) {
            return super.setException(throwable);
        }
    }
}