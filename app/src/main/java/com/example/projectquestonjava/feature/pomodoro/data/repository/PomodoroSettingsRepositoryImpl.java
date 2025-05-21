package com.example.projectquestonjava.feature.pomodoro.data.repository;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.example.projectquestonjava.core.managers.DataStoreManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSettingsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PomodoroSettingsRepositoryImpl implements PomodoroSettingsRepository {

    private static final Preferences.Key<Integer> WORK_DURATION_KEY = PreferencesKeys.intKey("pomodoro_work_duration");
    private static final Preferences.Key<Integer> BREAK_DURATION_KEY = PreferencesKeys.intKey("pomodoro_break_duration");
    private static final Preferences.Key<String> FOCUS_SOUND_URI_KEY = PreferencesKeys.stringKey("focus_sound_uri");
    private static final Preferences.Key<String> BREAK_SOUND_URI_KEY = PreferencesKeys.stringKey("break_sound_uri");
    private static final Preferences.Key<Boolean> VIBRATION_ENABLED_KEY = PreferencesKeys.booleanKey("vibration_enabled");

    private static final int DEFAULT_WORK_DURATION = 25;
    private static final int DEFAULT_BREAK_DURATION = 5;
    private static final boolean DEFAULT_VIBRATION_ENABLED = true; // Добавим значение по умолчанию

    private final DataStoreManager dataStoreManager;
    private final Logger logger;
    private final Executor directExecutor;

    @Inject
    public PomodoroSettingsRepositoryImpl(DataStoreManager dataStoreManager, Logger logger) {
        this.dataStoreManager = dataStoreManager;
        this.logger = logger;
        this.directExecutor = MoreExecutors.directExecutor();
    }

    @Override
    public LiveData<PomodoroSettings> getSettingsFlow() {
        MediatorLiveData<PomodoroSettings> settingsLiveData = new MediatorLiveData<>();

        LiveData<Integer> workDurationLiveData = dataStoreManager.getPreferenceLiveData(WORK_DURATION_KEY, DEFAULT_WORK_DURATION);
        LiveData<Integer> breakDurationLiveData = dataStoreManager.getPreferenceLiveData(BREAK_DURATION_KEY, DEFAULT_BREAK_DURATION);
        LiveData<String> focusSoundUriLiveData = dataStoreManager.getPreferenceLiveData(FOCUS_SOUND_URI_KEY, null); // null как defaultValue
        LiveData<String> breakSoundUriLiveData = dataStoreManager.getPreferenceLiveData(BREAK_SOUND_URI_KEY, null); // null как defaultValue
        LiveData<Boolean> vibrationEnabledLiveData = dataStoreManager.getPreferenceLiveData(VIBRATION_ENABLED_KEY, DEFAULT_VIBRATION_ENABLED);

        // Локальные переменные для хранения последних значений из LiveData
        final Integer[] workHolder = new Integer[1];
        final Integer[] breakHolder = new Integer[1];
        final String[] focusSoundHolder = new String[1];
        final String[] breakSoundHolder = new String[1];
        final Boolean[] vibrationHolder = new Boolean[1];
        final int[] sourcesInitializedCount = {0}; // Счетчик инициализированных источников

        Runnable updateCombinedSettings = () -> {
            if (sourcesInitializedCount[0] == 5) { // Обновляем только когда все 5 источников прислали значение
                settingsLiveData.setValue(new PomodoroSettings(
                        workHolder[0] != null ? workHolder[0] : DEFAULT_WORK_DURATION,
                        breakHolder[0] != null ? breakHolder[0] : DEFAULT_BREAK_DURATION,
                        focusSoundHolder[0], // Может быть null
                        breakSoundHolder[0], // Может быть null
                        vibrationHolder[0] != null ? vibrationHolder[0] : DEFAULT_VIBRATION_ENABLED
                ));
            }
        };

        settingsLiveData.addSource(workDurationLiveData, value -> {
            workHolder[0] = value;
            if (value != null && sourcesInitializedCount[0] < 5 && workHolder[0] == value) sourcesInitializedCount[0]++;
            updateCombinedSettings.run();
        });
        settingsLiveData.addSource(breakDurationLiveData, value -> {
            breakHolder[0] = value;
            if (value != null && sourcesInitializedCount[0] < 5 && breakHolder[0] == value) sourcesInitializedCount[0]++;
            updateCombinedSettings.run();
        });
        settingsLiveData.addSource(focusSoundUriLiveData, value -> {
            focusSoundHolder[0] = value;
            // Для nullable полей, считаем инициализированным, даже если value null
            if (sourcesInitializedCount[0] < 5 && (focusSoundHolder[0] == value || focusSoundHolder[0] == null)) sourcesInitializedCount[0]++;
            updateCombinedSettings.run();
        });
        settingsLiveData.addSource(breakSoundUriLiveData, value -> {
            breakSoundHolder[0] = value;
            if (sourcesInitializedCount[0] < 5 && (breakSoundHolder[0] == value || breakSoundHolder[0] == null)) sourcesInitializedCount[0]++;
            updateCombinedSettings.run();
        });
        settingsLiveData.addSource(vibrationEnabledLiveData, value -> {
            vibrationHolder[0] = value;
            if (value != null && sourcesInitializedCount[0] < 5 && vibrationHolder[0] == value) sourcesInitializedCount[0]++;
            updateCombinedSettings.run();
        });

        return settingsLiveData;
    }

    @Override
    public ListenableFuture<PomodoroSettings> getSettings() {
        ListenableFuture<Integer> workFuture = dataStoreManager.getValueFuture(WORK_DURATION_KEY, DEFAULT_WORK_DURATION);
        ListenableFuture<Integer> breakFuture = dataStoreManager.getValueFuture(BREAK_DURATION_KEY, DEFAULT_BREAK_DURATION);
        ListenableFuture<String> focusSoundFuture = dataStoreManager.getValueFuture(FOCUS_SOUND_URI_KEY, null);
        ListenableFuture<String> breakSoundFuture = dataStoreManager.getValueFuture(BREAK_SOUND_URI_KEY, null);
        ListenableFuture<Boolean> vibrationFuture = dataStoreManager.getValueFuture(VIBRATION_ENABLED_KEY, DEFAULT_VIBRATION_ENABLED);

        List<ListenableFuture<?>> futuresList = new ArrayList<>();
        futuresList.add(workFuture);
        futuresList.add(breakFuture);
        futuresList.add(focusSoundFuture);
        futuresList.add(breakSoundFuture);
        futuresList.add(vibrationFuture);

        return Futures.transform(Futures.allAsList(futuresList), results -> {
            if (results == null || results.size() < 5) try {
                throw new IOException("Failed to read all settings from DataStore");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Явное приведение типов, так как allAsList возвращает List<Object>
            Integer work = (Integer) results.get(0);
            Integer breakD = (Integer) results.get(1);
            String focusSound = (String) results.get(2);
            String breakSound = (String) results.get(3);
            Boolean vibration = (Boolean) results.get(4);

            return new PomodoroSettings(
                    work != null ? work : DEFAULT_WORK_DURATION,
                    breakD != null ? breakD : DEFAULT_BREAK_DURATION,
                    focusSound,
                    breakSound,
                    vibration != null ? vibration : DEFAULT_VIBRATION_ENABLED
            );
        }, directExecutor);
    }

    @Override
    public ListenableFuture<Void> saveSettings(PomodoroSettings settings) {
        List<ListenableFuture<Void>> saveOperations = new ArrayList<>();
        saveOperations.add(dataStoreManager.saveValueFuture(WORK_DURATION_KEY, settings.getWorkDurationMinutes()));
        saveOperations.add(dataStoreManager.saveValueFuture(BREAK_DURATION_KEY, settings.getBreakDurationMinutes()));

        if (settings.getFocusSoundUri() != null) {
            saveOperations.add(dataStoreManager.saveValueFuture(FOCUS_SOUND_URI_KEY, settings.getFocusSoundUri()));
        } else {
            saveOperations.add(dataStoreManager.clearValueFuture(FOCUS_SOUND_URI_KEY));
        }
        if (settings.getBreakSoundUri() != null) {
            saveOperations.add(dataStoreManager.saveValueFuture(BREAK_SOUND_URI_KEY, settings.getBreakSoundUri()));
        } else {
            saveOperations.add(dataStoreManager.clearValueFuture(BREAK_SOUND_URI_KEY));
        }
        saveOperations.add(dataStoreManager.saveValueFuture(VIBRATION_ENABLED_KEY, settings.isVibrationEnabled()));

        return Futures.transform(Futures.allAsList(saveOperations), input -> null, directExecutor);
    }
}