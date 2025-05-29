package com.example.projectquestonjava.core.managers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.NoSuchElementException;
import java.util.Objects;
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
    private final Executor ioExecutor;

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
     * @param defaultValue Значение по умолчанию. Для строковых ключей, если вы хотите, чтобы LiveData эмитил null,
     *                     когда ключ отсутствует, передайте сюда специальное не-null значение (например, пустую строку),
     *                     а затем преобразуйте его в null в Transformations.map в вашем репозитории или ViewModel.
     *                     Либо, если тип T это Optional<String>, то можно передавать Optional.empty().
     *                     RxJava map оператор не должен возвращать null.
     * @param <T>          Тип значения.
     * @return LiveData, эмитящий значения.
     */
    public <T> LiveData<T> getPreferenceLiveData(@NonNull Preferences.Key<T> key, @NonNull T defaultValue) {
        Flowable<T> flowable = dataStore.data()
                .map(prefs -> {
                    T value = prefs.get(key);
                    // map не должен возвращать null. Если value null, мы обязаны вернуть defaultValue.
                    // defaultValue здесь должен быть не-null.
                    return value != null ? value : defaultValue;
                })
                .onErrorReturn(error -> {
                    logger.error(TAG, "Error reading preference for key: " + key.getName() + " in LiveData stream. Returning defaultValue.", error);
                    return defaultValue;
                })
                .subscribeOn(Schedulers.from(ioExecutor));

        return LiveDataReactiveStreams.fromPublisher(flowable);
    }


    public <T> ListenableFuture<T> getValueFuture(@NonNull Preferences.Key<T> key, @Nullable T defaultValue) {
        SettableFuture<T> settableFuture = SettableFuture.create();
        Disposable disposable = dataStore.data().firstOrError()
                .map(prefs -> {
                    T value = prefs.get(key);
                    if (value != null) {
                        return value;
                    }
                    // Если значение отсутствует и defaultValue предоставлен (не null), возвращаем его.
                    if (defaultValue != null) {
                        return defaultValue;
                    }
                    // Если значение отсутствует и defaultValue тоже null, Future должен завершиться ошибкой.
                    throw new NoSuchElementException("Key " + key.getName() + " not found and no non-null defaultValue provided.");
                })
                .subscribeOn(Schedulers.from(ioExecutor))
                .subscribe(
                        settableFuture::set,
                        throwable -> {
                            if (throwable instanceof NoSuchElementException) {
                                // Это ожидаемая ошибка, если ключ не найден и defaultValue был null.
                                // Завершаем Future с этой ошибкой.
                                logger.warn(TAG, throwable.getMessage() + " (Future fails with NoSuchElementException).");
                                settableFuture.setException(throwable);
                            } else {
                                logger.error(TAG, "Error getting value for key: " + key.getName() + " with Future.", throwable);
                                settableFuture.setException(new IOException("Failed to get value for " + key.getName(), throwable));
                            }
                        }
                );

        settableFuture.addListener(() -> {
            if (settableFuture.isCancelled() && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }, ioExecutor);
        return settableFuture;
    }
    // ... (saveValueFuture, clearValueFuture без изменений) ...
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