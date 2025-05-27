package com.example.projectquestonjava.feature.gamification.data.managers;

import androidx.annotation.NonNull;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations; // Для Transformations.map

import com.example.projectquestonjava.core.data.converters.Converters;
import com.example.projectquestonjava.core.managers.DataStoreManager;
import com.example.projectquestonjava.core.utils.Logger;
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
    private final Executor directExecutor; // Для коллбэков

    @Inject
    public GamificationDataStoreManager(DataStoreManager dataStoreManager,
                                        Logger logger,
                                        @com.example.projectquestonjava.core.di.IODispatcher Executor ioExecutor) { // IODispatcher используется для DataStoreManager
        this.dataStoreManager = dataStoreManager;
        this.logger = logger;
        this.directExecutor = MoreExecutors.directExecutor();
    }

    // --- Методы для gamification_id ---
    public ListenableFuture<Void> clearGamificationId() {
        return dataStoreManager.clearValueFuture(GAMIFICATION_ID_KEY);
    }

    public ListenableFuture<Void> clearAllGamificationData() {
        return dataStoreManager.clearValueFuture(GAMIFICATION_ID_KEY);
    }

    public ListenableFuture<Void> saveGamificationId(long id) {
        return dataStoreManager.saveValueFuture(GAMIFICATION_ID_KEY, id);
    }

    public ListenableFuture<Long> getGamificationIdFuture() {
        return dataStoreManager.getValueFuture(GAMIFICATION_ID_KEY, -1L);
    }

    public long getGamificationIdSync() throws IOException {
        try {
            return dataStoreManager.getValueFuture(GAMIFICATION_ID_KEY, -1L).get();
        } catch (Exception e) {
            logger.error("GamificationDataStoreManager", "Error getting gamification_id sync", e);
            throw new IOException("Failed to get gamification_id", e);
        }
    }

    public LiveData<Long> getGamificationIdFlow() {
        return dataStoreManager.getPreferenceLiveData(GAMIFICATION_ID_KEY, -1L);
    }

    // --- Методы для selected_plant_id ---
    public ListenableFuture<Long> getSelectedPlantIdFuture() {
        return dataStoreManager.getValueFuture(SELECTED_PLANT_ID_KEY, -1L);
    }

    public long getSelectedPlantIdSync() throws IOException {
        try {
            return dataStoreManager.getValueFuture(SELECTED_PLANT_ID_KEY, -1L).get();
        } catch (Exception e) {
            logger.error("GamificationDataStoreManager", "Error getting selected_plant_id sync", e);
            throw new IOException("Failed to get selected_plant_id", e);
        }
    }


    public LiveData<Long> getSelectedPlantIdFlow() {
        return dataStoreManager.getPreferenceLiveData(SELECTED_PLANT_ID_KEY, -1L);
    }

    public ListenableFuture<Void> saveSelectedPlantId(long id) {
        return dataStoreManager.saveValueFuture(SELECTED_PLANT_ID_KEY, id);
    }

    public ListenableFuture<Void> clearSelectedPlantId() {
        return dataStoreManager.clearValueFuture(SELECTED_PLANT_ID_KEY);
    }

    // --- Методы для surprise_task ---
    public ListenableFuture<LocalDateTime> getStartTimeSurpriseTaskFuture() {
        Converters converter = new Converters();
        return Futures.transform(
                dataStoreManager.getValueFuture(START_TIME_SURPRISE_TASK_KEY, -1L),
                timestamp -> timestamp == -1L ? null : converter.fromTimestamp(timestamp), // Проверка на null для timestamp
                directExecutor
        );
    }
    public LocalDateTime getStartTimeSurpriseTaskSync() throws IOException {
        Converters converter = new Converters();
        try {
            Long timestamp = dataStoreManager.getValueFuture(START_TIME_SURPRISE_TASK_KEY, -1L).get();
            return timestamp == -1L ? null : converter.fromTimestamp(timestamp);
        } catch (Exception e) {
            logger.error("GamificationDataStoreManager", "Error getting start_time_surprise_task sync", e);
            throw new IOException("Failed to get start_time_surprise_task", e);
        }
    }


    public LiveData<LocalDateTime> getStartTimeSurpriseTaskFlow() {
        Converters converter = new Converters();
        return Transformations.map(dataStoreManager.getPreferenceLiveData(START_TIME_SURPRISE_TASK_KEY, -1L),
                timestamp -> timestamp == -1L ? null : converter.fromTimestamp(timestamp));
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
        ListenableFuture<Set<String>> currentIdsFuture = dataStoreManager.getValueFuture(HIDDEN_EXPIRED_TASK_IDS_KEY, Collections.emptySet());

        return Futures.transformAsync(currentIdsFuture, currentIds -> {
            Set<String> newIds = new HashSet<>(currentIds != null ? currentIds : Collections.emptySet());
            newIds.add(taskIdString);
            return dataStoreManager.saveValueFuture(HIDDEN_EXPIRED_TASK_IDS_KEY, newIds);
        }, directExecutor); // Используем directExecutor, так как логика простая
    }

    public LiveData<Set<Long>> getHiddenExpiredTaskIdsFlow() {
        return Transformations.map(dataStoreManager.getPreferenceLiveData(HIDDEN_EXPIRED_TASK_IDS_KEY, Collections.emptySet()),
                stringSet -> {
                    if (stringSet == null) return Collections.emptySet();
                    return stringSet.stream()
                            .map(s -> {
                                try { return Long.parseLong(s); }
                                catch (NumberFormatException e) {
                                    logger.warn("GamificationDataStoreManager", "Invalid long string in hiddenExpiredTaskIds: " + s);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                });
    }

    public ListenableFuture<Void> clearHiddenExpiredTaskIds() {
        return dataStoreManager.clearValueFuture(HIDDEN_EXPIRED_TASK_IDS_KEY);
    }
}