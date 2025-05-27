package com.example.projectquestonjava.core.managers;

import androidx.annotation.NonNull;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public class DataStoreManager {

    private static final String TAG = "DataStoreManager";
    private final RxDataStore<Preferences> dataStore;
    private final Logger logger;
    private final Executor ioExecutor; // Для выполнения операций DataStore

    @Inject
    public DataStoreManager(
            RxDataStore<Preferences> dataStore,
            Logger logger,
            @IODispatcher Executor ioExecutor) {
        this.dataStore = dataStore;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
    }

    /**
     * Получает LiveData для значения из DataStore.
     *
     * @param key          Ключ Preferences.
     * @param defaultValue Значение по умолчанию, если ключ отсутствует.
     * @param <T>          Тип значения.
     * @return LiveData, эмитящий значения.
     */
    public <T> LiveData<T> getPreferenceLiveData(@NonNull Preferences.Key<T> key, T defaultValue) {
        Flowable<T> flowable = dataStore.data()
                .map(prefs -> {
                    // Objects.requireNonNullElse() требует API 24, используем явную проверку
                    T value = prefs.get(key);
                    return value != null ? value : defaultValue;
                })
                .onErrorReturn(error -> {
                    logger.error(TAG, "Error reading preference for key: " + key.getName(), error);
                    return defaultValue; // Возвращаем значение по умолчанию при ошибке
                })
                .subscribeOn(Schedulers.from(ioExecutor)); // Выполняем чтение на IO потоке

        return LiveDataReactiveStreams.fromPublisher(flowable);
    }

    /**
     * Получает значение из DataStore асинхронно, возвращая ListenableFuture.
     *
     * @param key          Ключ Preferences.
     * @param defaultValue Значение по умолчанию.
     * @param <T>          Тип значения.
     * @return ListenableFuture с результатом.
     */
    public <T> ListenableFuture<T> getValueFuture(@NonNull Preferences.Key<T> key, T defaultValue) {
        SettableFuture<T> settableFuture = SettableFuture.create();
        Disposable disposable = dataStore.data().firstOrError()
                .map(prefs -> {
                    T value = prefs.get(key);
                    return value != null ? value : defaultValue;
                })
                .subscribeOn(Schedulers.from(ioExecutor))
                .subscribe(
                        settableFuture::set, // Успех
                        throwable -> {      // Ошибка
                            logger.error(TAG, "Error getting value for key: " + key.getName() + " with Future", throwable);
                            settableFuture.setException(new IOException("Failed to get value for " + key.getName(), throwable));
                        }
                );

        settableFuture.addListener(() -> {
            if (settableFuture.isCancelled() && !disposable.isDisposed()) {
                disposable.dispose();
                logger.debug(TAG, "RxJava subscription disposed due to ListenableFuture cancellation for key: " + key.getName());
            }
        }, ioExecutor); // Используем ioExecutor или MoreExecutors.directExecutor()
        return settableFuture;
    }


    /**
     * Сохраняет значение в DataStore асинхронно.
     *
     * @param key   Ключ Preferences.
     * @param value Значение для сохранения.
     * @param <T>   Тип значения.
     * @return ListenableFuture<Void> для отслеживания завершения.
     */
    public <T> ListenableFuture<Void> saveValueFuture(@NonNull Preferences.Key<T> key, @NonNull T value) {
        SettableFuture<Void> settableFuture = SettableFuture.create();
        Disposable disposable = dataStore.updateDataAsync(prefsIn -> {
                    MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
                    mutablePreferences.set(key, value);
                    return Single.just(mutablePreferences.toPreferences());
                })
                .subscribeOn(Schedulers.from(ioExecutor))
                .subscribe(
                        prefs -> {
                            logger.debug(TAG, "Value updated in DataStore: " + key.getName() + " -> " + value);
                            settableFuture.set(null);
                        },
                        throwable -> {
                            logger.error(TAG, "Error saving value for key: " + key.getName(), throwable);
                            settableFuture.setException(new IOException("Failed to save value for " + key.getName(), throwable));
                        }
                );
        settableFuture.addListener(() -> {
            if (settableFuture.isCancelled() && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }, ioExecutor);
        return settableFuture;
    }

    /**
     * Очищает значение из DataStore асинхронно.
     *
     * @param key Ключ Preferences для удаления.
     * @param <T> Тип значения (не используется при удалении, но нужен для сигнатуры ключа).
     * @return ListenableFuture<Void> для отслеживания завершения.
     */
    public <T> ListenableFuture<Void> clearValueFuture(@NonNull Preferences.Key<T> key) {
        SettableFuture<Void> settableFuture = SettableFuture.create();
        Disposable disposable = dataStore.updateDataAsync(prefsIn -> {
                    MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
                    mutablePreferences.remove(key);
                    return Single.just(mutablePreferences.toPreferences());
                })
                .subscribeOn(Schedulers.from(ioExecutor))
                .subscribe(
                        prefs -> {
                            logger.debug(TAG, "Value cleared in DataStore: " + key.getName());
                            settableFuture.set(null);
                        },
                        throwable -> {
                            logger.error(TAG, "Error clearing value for key: " + key.getName(), throwable);
                            settableFuture.setException(new IOException("Failed to clear value for " + key.getName(), throwable));
                        }
                );
        settableFuture.addListener(() -> {
            if (settableFuture.isCancelled() && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }, ioExecutor);
        return settableFuture;
    }
}