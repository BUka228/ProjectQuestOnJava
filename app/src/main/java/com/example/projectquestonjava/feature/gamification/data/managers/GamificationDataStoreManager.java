package com.example.projectquestonjava.feature.gamification.data.managers;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.projectquestonjava.core.data.converters.Converters;
import com.example.projectquestonjava.core.managers.DataStoreManager;
import com.example.projectquestonjava.core.utils.Logger; // Добавим Logger
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GamificationDataStoreManager {

    private static final Preferences.Key<Long> GAMIFICATION_ID_KEY = PreferencesKeys.longKey("gamification_id");
    private static final Preferences.Key<Long> SELECTED_PLANT_ID_KEY = PreferencesKeys.longKey("selected_plant_id");
    private static final Preferences.Key<Long> START_TIME_SURPRISE_TASK_KEY = PreferencesKeys.longKey("start_time_surprise_task");
    private static final Preferences.Key<Set<String>> HIDDEN_EXPIRED_TASK_IDS_KEY = PreferencesKeys.stringSetKey("hidden_expired_surprise_task_ids");

    private final DataStoreManager dataStoreManager;
    private final Logger logger;
    private final Executor ioExecutor; // Для getValueFuture и других асинхронных операций DataStore

    @Inject
    public GamificationDataStoreManager(DataStoreManager dataStoreManager,
                                        Logger logger,
                                        @com.example.projectquestonjava.core.di.IODispatcher Executor ioExecutor) {
        this.dataStoreManager = dataStoreManager;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
    }

    // --- Методы для gamification_id ---
    public ListenableFuture<Void> clearGamificationId() {
        return dataStoreManager.clearValueFuture(GAMIFICATION_ID_KEY);
    }

    public ListenableFuture<Void> saveGamificationId(long id) {
        return dataStoreManager.saveValueFuture(GAMIFICATION_ID_KEY, id);
    }

    public ListenableFuture<Long> getGamificationId() { // Возвращаем Future
        return dataStoreManager.getValueFuture(GAMIFICATION_ID_KEY, -1L);
    }
    // Синхронный метод, если нужен (но использовать с осторожностью)
    public long getGamificationIdSync() throws IOException {
        return dataStoreManager.getValue(GAMIFICATION_ID_KEY, -1L);
    }


    public LiveData<Long> getGamificationIdFlow() { // Возвращаем LiveData
        return dataStoreManager.getFlow(GAMIFICATION_ID_KEY, -1L);
    }

    // --- Методы для selected_plant_id ---
    public ListenableFuture<Long> getSelectedPlantId() {
        return dataStoreManager.getValueFuture(SELECTED_PLANT_ID_KEY, -1L);
    }
    public long getSelectedPlantIdSync() throws IOException {
        return dataStoreManager.getValue(SELECTED_PLANT_ID_KEY, -1L);
    }


    public LiveData<Long> getSelectedPlantIdFlow() {
        return dataStoreManager.getFlow(SELECTED_PLANT_ID_KEY, -1L);
    }

    public ListenableFuture<Void> saveSelectedPlantId(long id) {
        return dataStoreManager.saveValueFuture(SELECTED_PLANT_ID_KEY, id);
    }

    public ListenableFuture<Void> clearSelectedPlantId() {
        return dataStoreManager.clearValueFuture(SELECTED_PLANT_ID_KEY);
    }

    // --- Методы для surprise_task ---
    public ListenableFuture<LocalDateTime> getStartTimeSurpriseTask() {
        Converters converter = new Converters();
        return Futures.transform(
                dataStoreManager.getValueFuture(START_TIME_SURPRISE_TASK_KEY, -1L),
                timestamp -> (timestamp == -1L) ? null : converter.fromTimestamp(timestamp),
                MoreExecutors.directExecutor()
        );
    }
    public LocalDateTime getStartTimeSurpriseTaskSync() throws IOException {
        Converters converter = new Converters();
        Long timestamp = dataStoreManager.getValue(START_TIME_SURPRISE_TASK_KEY, -1L);
        return (timestamp == -1L) ? null : converter.fromTimestamp(timestamp);
    }


    public LiveData<LocalDateTime> getStartTimeSurpriseTaskFlow() {
        Converters converter = new Converters();
        return Transformations.map(dataStoreManager.getFlow(START_TIME_SURPRISE_TASK_KEY, -1L),
                timestamp -> (timestamp == -1L || timestamp == null) ? null : converter.fromTimestamp(timestamp));
    }

    public ListenableFuture<Void> saveStartTimeSurpriseTask(LocalDateTime time) {
        Converters converter = new Converters();
        Long timestamp = converter.toTimestamp(time);
        if (timestamp == null) {
            return Futures.immediateFailedFuture(new IllegalArgumentException("Cannot convert null LocalDateTime to timestamp"));
        }
        return dataStoreManager.saveValueFuture(START_TIME_SURPRISE_TASK_KEY, timestamp);
    }

    public ListenableFuture<Void> clearStartTimeSurpriseTask() {
        return dataStoreManager.clearValueFuture(START_TIME_SURPRISE_TASK_KEY);
    }

    public ListenableFuture<Void> hideExpiredTaskId(long taskId) {
        String taskIdString = String.valueOf(taskId);
        // Асинхронное чтение и затем асинхронная запись
        return Futures.transformAsync(
                dataStoreManager.getValueFuture(HIDDEN_EXPIRED_TASK_IDS_KEY, Collections.emptySet()),
                currentIds -> {
                    Set<String> newIds = new HashSet<>(currentIds != null ? currentIds : Collections.emptySet());
                    newIds.add(taskIdString);
                    return dataStoreManager.saveValueFuture(HIDDEN_EXPIRED_TASK_IDS_KEY, newIds);
                },
                ioExecutor // Используем ioExecutor для transformAsync
        );
    }

    public LiveData<Set<Long>> getHiddenExpiredTaskIdsFlow() {
        return Transformations.map(dataStoreManager.getFlow(HIDDEN_EXPIRED_TASK_IDS_KEY, Collections.<String>emptySet()),
                stringSet -> {
                    if (stringSet == null) return Collections.emptySet();
                    return stringSet.stream()
                            .map(s -> {
                                try { return Long.parseLong(s); }
                                catch (NumberFormatException e) { return null; }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                });
    }


    public ListenableFuture<Void> clearHiddenExpiredTaskIds() {
        return dataStoreManager.clearValueFuture(HIDDEN_EXPIRED_TASK_IDS_KEY);
    }
}