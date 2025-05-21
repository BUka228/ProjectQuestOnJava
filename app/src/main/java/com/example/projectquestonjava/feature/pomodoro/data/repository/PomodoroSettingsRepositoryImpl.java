package com.example.projectquestonjava.feature.pomodoro.data.repository;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSettingsRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor; // Если DataStore операции выполняются на нем
import javax.inject.Inject;
import javax.inject.Singleton;


// Для доступа к DataStore напрямую или через DataStoreManager
import com.example.projectquestonjava.core.managers.DataStoreManager;

@Singleton
public class PomodoroSettingsRepositoryImpl implements PomodoroSettingsRepository {

    private static final Preferences.Key<Integer> WORK_DURATION_KEY = PreferencesKeys.intKey("pomodoro_work_duration");
    private static final Preferences.Key<Integer> BREAK_DURATION_KEY = PreferencesKeys.intKey("pomodoro_break_duration");
    private static final Preferences.Key<String> FOCUS_SOUND_URI_KEY = PreferencesKeys.stringKey("focus_sound_uri");
    private static final Preferences.Key<String> BREAK_SOUND_URI_KEY = PreferencesKeys.stringKey("break_sound_uri");
    private static final Preferences.Key<Boolean> VIBRATION_ENABLED_KEY = PreferencesKeys.booleanKey("vibration_enabled");

    private static final int DEFAULT_WORK_DURATION = 25;
    private static final int DEFAULT_BREAK_DURATION = 5;

    private final DataStoreManager dataStoreManager; // Используем DataStoreManager
    private final Logger logger;
    // private final Executor ioExecutor; // Если DataStoreManager требует явного Executor

    @Inject
    public PomodoroSettingsRepositoryImpl(DataStoreManager dataStoreManager, Logger logger) {
        this.dataStoreManager = dataStoreManager;
        this.logger = logger;
    }

    @Override
    public LiveData<PomodoroSettings> getSettingsFlow() {
        // DataStoreManager.getFlow должен возвращать LiveData или Flow, который мы конвертируем
        // Предположим, что DataStoreManager.getFlow возвращает Kotlin Flow
        // и мы его конвертируем в LiveData здесь (или в DataStoreManager)
        // Это временное решение, DataStoreManager тоже будет переписан.

        // Создаем LiveData, который будет обновляться при изменении любого из ключей
        MediatorLiveData<PomodoroSettings> settingsLiveData = new MediatorLiveData<>();

        LiveData<Integer> workDurationLiveData = dataStoreManager.getFlow(WORK_DURATION_KEY, DEFAULT_WORK_DURATION);
        LiveData<Integer> breakDurationLiveData = dataStoreManager.getFlow(BREAK_DURATION_KEY, DEFAULT_BREAK_DURATION);
        LiveData<String> focusSoundUriLiveData = dataStoreManager.getFlow(FOCUS_SOUND_URI_KEY, null);
        LiveData<String> breakSoundUriLiveData = dataStoreManager.getFlow(BREAK_SOUND_URI_KEY, null);
        LiveData<Boolean> vibrationEnabledLiveData = dataStoreManager.getFlow(VIBRATION_ENABLED_KEY, true);

        Runnable updateCombinedSettings = () -> {
            Integer work = workDurationLiveData.getValue();
            Integer breakD = breakDurationLiveData.getValue();
            String focusSound = focusSoundUriLiveData.getValue();
            String breakSound = breakSoundUriLiveData.getValue();
            Boolean vibration = vibrationEnabledLiveData.getValue();

            // Только если все значения загружены
            if (work != null && breakD != null && vibration != null) { // Звуки могут быть null
                settingsLiveData.setValue(new PomodoroSettings(work, breakD, focusSound, breakSound, vibration));
            }
        };

        settingsLiveData.addSource(workDurationLiveData, value -> updateCombinedSettings.run());
        settingsLiveData.addSource(breakDurationLiveData, value -> updateCombinedSettings.run());
        settingsLiveData.addSource(focusSoundUriLiveData, value -> updateCombinedSettings.run());
        settingsLiveData.addSource(breakSoundUriLiveData, value -> updateCombinedSettings.run());
        settingsLiveData.addSource(vibrationEnabledLiveData, value -> updateCombinedSettings.run());

        return settingsLiveData;
    }


    @Override
    public ListenableFuture<PomodoroSettings> getSettings() {
        // Аналогично, DataStoreManager.getValue должен быть адаптирован
        // Здесь для примера, как бы это могло выглядеть с ListenableFuture
        // В реальности DataStore асинхронен.
        ListenableFuture<Integer> workFuture = dataStoreManager.getValueFuture(WORK_DURATION_KEY, DEFAULT_WORK_DURATION);
        ListenableFuture<Integer> breakFuture = dataStoreManager.getValueFuture(BREAK_DURATION_KEY, DEFAULT_BREAK_DURATION);
        ListenableFuture<String> focusSoundFuture = dataStoreManager.getValueFuture(FOCUS_SOUND_URI_KEY, null);
        ListenableFuture<String> breakSoundFuture = dataStoreManager.getValueFuture(BREAK_SOUND_URI_KEY, null);
        ListenableFuture<Boolean> vibrationFuture = dataStoreManager.getValueFuture(VIBRATION_ENABLED_KEY, true);

        ListenableFuture<List<Object>> allFutures = Futures.allAsList(workFuture, breakFuture, focusSoundFuture, breakSoundFuture, vibrationFuture);

        return Futures.transform(allFutures, results -> {
            if (results == null) throw new IOException("Failed to read settings from DataStore");
            return new PomodoroSettings(
                    (Integer) results.get(0),
                    (Integer) results.get(1),
                    (String) results.get(2),
                    (String) results.get(3),
                    (Boolean) results.get(4)
            );
        }, MoreExecutors.directExecutor()); // Преобразование в том же потоке, что и последний future
    }

    @Override
    public ListenableFuture<Void> saveSettings(PomodoroSettings settings) {
        // DataStoreManager.saveValue должен возвращать ListenableFuture или использовать колбэк
        // Здесь мы имитируем последовательное сохранение.
        // В идеале DataStoreManager.edit должен быть атомарной операцией.

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

        return Futures.transform(Futures.allAsList(saveOperations), input -> null, MoreExecutors.directExecutor());
    }
}