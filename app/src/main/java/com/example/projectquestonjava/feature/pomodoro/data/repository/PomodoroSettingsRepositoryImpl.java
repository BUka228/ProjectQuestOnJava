package com.example.projectquestonjava.feature.pomodoro.data.repository;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations; // Добавлен импорт
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
import java.util.NoSuchElementException;
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

    private static final String TAG = "PomodoroSettingsRepo";

    private static final int DEFAULT_WORK_DURATION = 25;
    private static final int DEFAULT_BREAK_DURATION = 5;
    // Используем пустую строку как defaultValue для DataStoreManager, если хотим эмитить null в LiveData
    private static final String EMPTY_STRING_DEFAULT = "";
    private static final boolean DEFAULT_VIBRATION_ENABLED = true;

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

        // DataStoreManager.getPreferenceLiveData теперь ожидает НЕ-NULL defaultValue.
        // Для строк, где null означает "не установлено", передаем EMPTY_STRING_DEFAULT.
        LiveData<Integer> workDurationLiveData = dataStoreManager.getPreferenceLiveData(WORK_DURATION_KEY, DEFAULT_WORK_DURATION);
        LiveData<Integer> breakDurationLiveData = dataStoreManager.getPreferenceLiveData(BREAK_DURATION_KEY, DEFAULT_BREAK_DURATION);
        LiveData<String> focusSoundUriFromStoreLiveData = dataStoreManager.getPreferenceLiveData(FOCUS_SOUND_URI_KEY, EMPTY_STRING_DEFAULT);
        LiveData<String> breakSoundUriFromStoreLiveData = dataStoreManager.getPreferenceLiveData(BREAK_SOUND_URI_KEY, EMPTY_STRING_DEFAULT);
        LiveData<Boolean> vibrationEnabledLiveData = dataStoreManager.getPreferenceLiveData(VIBRATION_ENABLED_KEY, DEFAULT_VIBRATION_ENABLED);

        // Преобразуем пустые строки в null для URI
        LiveData<String> focusSoundUriLiveData = Transformations.map(focusSoundUriFromStoreLiveData, uri -> uri.isEmpty() ? null : uri);
        LiveData<String> breakSoundUriLiveData = Transformations.map(breakSoundUriFromStoreLiveData, uri -> uri.isEmpty() ? null : uri);


        final Integer[] workHolder = {null};
        final Integer[] breakHolder = {null};
        // Для focusSoundHolder и breakSoundHolder, мы теперь можем ожидать null после Transformations.map
        final String[] focusSoundHolder = {null}; // Может быть null, это его начальное "не загруженное" состояние
        final boolean[] focusSoundReported = {false}; // Отдельный флаг для отслеживания первого эмита (включая null)
        final String[] breakSoundHolder = {null};
        final boolean[] breakSoundReported = {false};
        final Boolean[] vibrationHolder = {null};
        final int[] sourcesReported = {0};
        final int TOTAL_SOURCES = 5;

        Runnable updateCombinedSettings = () -> {
            if (sourcesReported[0] == TOTAL_SOURCES && focusSoundReported[0] && breakSoundReported[0]) {
                settingsLiveData.setValue(new PomodoroSettings(
                        workHolder[0] != null ? workHolder[0] : DEFAULT_WORK_DURATION,
                        breakHolder[0] != null ? breakHolder[0] : DEFAULT_BREAK_DURATION,
                        focusSoundHolder[0], // Теперь может быть null напрямую
                        breakSoundHolder[0], // Теперь может быть null напрямую
                        vibrationHolder[0] != null ? vibrationHolder[0] : DEFAULT_VIBRATION_ENABLED
                ));
            }
        };

        settingsLiveData.addSource(workDurationLiveData, value -> {
            if (workHolder[0] == null && value != null) sourcesReported[0]++; // Считаем только первый не-null эмит
            workHolder[0] = value;
            updateCombinedSettings.run();
        });
        settingsLiveData.addSource(breakDurationLiveData, value -> {
            if (breakHolder[0] == null && value != null) sourcesReported[0]++;
            breakHolder[0] = value;
            updateCombinedSettings.run();
        });
        settingsLiveData.addSource(focusSoundUriLiveData, value -> { // Подписываемся на преобразованный LiveData
            if (!focusSoundReported[0]) {
                sourcesReported[0]++;
                focusSoundReported[0] = true;
            }
            focusSoundHolder[0] = value; // value теперь может быть null
            updateCombinedSettings.run();
        });
        settingsLiveData.addSource(breakSoundUriLiveData, value -> { // Подписываемся на преобразованный LiveData
            if (!breakSoundReported[0]) {
                sourcesReported[0]++;
                breakSoundReported[0] = true;
            }
            breakSoundHolder[0] = value; // value теперь может быть null
            updateCombinedSettings.run();
        });
        settingsLiveData.addSource(vibrationEnabledLiveData, value -> {
            if (vibrationHolder[0] == null && value != null) sourcesReported[0]++;
            vibrationHolder[0] = value;
            updateCombinedSettings.run();
        });

        return settingsLiveData;
    }


    @Override
    public ListenableFuture<PomodoroSettings> getSettings() {
        // DataStoreManager.getValueFuture для строк теперь бросит NoSuchElementException, если ключ не найден и defaultValue null.
        // Мы используем Futures.catching для обработки этого и возврата дефолтного значения.
        ListenableFuture<Integer> workFuture = dataStoreManager.getValueFuture(WORK_DURATION_KEY, DEFAULT_WORK_DURATION);
        ListenableFuture<Integer> breakFuture = dataStoreManager.getValueFuture(BREAK_DURATION_KEY, DEFAULT_BREAK_DURATION);
        // Для строковых URI, если они не найдены, getValueFuture с defaultValue=null бросит NoSuchElementException.
        // Мы перехватываем это и возвращаем null (наш реальный default для URI).
        ListenableFuture<String> focusSoundFuture = Futures.catching(
                dataStoreManager.getValueFuture(FOCUS_SOUND_URI_KEY, null), // Передаем null как defaultValue в DataStoreManager
                NoSuchElementException.class, e -> null, directExecutor
        );
        ListenableFuture<String> breakSoundFuture = Futures.catching(
                dataStoreManager.getValueFuture(BREAK_SOUND_URI_KEY, null),
                NoSuchElementException.class, e -> null, directExecutor
        );
        ListenableFuture<Boolean> vibrationFuture = dataStoreManager.getValueFuture(VIBRATION_ENABLED_KEY, DEFAULT_VIBRATION_ENABLED);

        List<ListenableFuture<?>> futuresList = new ArrayList<>();
        futuresList.add(workFuture);
        futuresList.add(breakFuture);
        futuresList.add(focusSoundFuture);
        futuresList.add(breakSoundFuture);
        futuresList.add(vibrationFuture);

        return Futures.transform(Futures.allAsList(futuresList), results -> {
            if (results == null || results.size() < 5) {
                logger.error(TAG, "Failed to read all settings from DataStore, results list is null or incomplete.");
                return new PomodoroSettings(DEFAULT_WORK_DURATION, DEFAULT_BREAK_DURATION, null, null, DEFAULT_VIBRATION_ENABLED);
            }
            Integer work = (Integer) results.get(0);
            Integer breakD = (Integer) results.get(1);
            String focusSound = (String) results.get(2); // Может быть null
            String breakSound = (String) results.get(3); // Может быть null
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
    // ... (saveSettings без изменений) ...
    @Override
    public ListenableFuture<Void> saveSettings(PomodoroSettings settings) {
        List<ListenableFuture<Void>> saveOperations = new ArrayList<>();
        saveOperations.add(dataStoreManager.saveValueFuture(WORK_DURATION_KEY, settings.getWorkDurationMinutes()));
        saveOperations.add(dataStoreManager.saveValueFuture(BREAK_DURATION_KEY, settings.getBreakDurationMinutes()));

        if (settings.getFocusSoundUri() != null && !settings.getFocusSoundUri().isEmpty()) { // Проверяем на непустоту перед сохранением
            saveOperations.add(dataStoreManager.saveValueFuture(FOCUS_SOUND_URI_KEY, settings.getFocusSoundUri()));
        } else {
            saveOperations.add(dataStoreManager.clearValueFuture(FOCUS_SOUND_URI_KEY));
        }
        if (settings.getBreakSoundUri() != null && !settings.getBreakSoundUri().isEmpty()) { // Проверяем на непустоту
            saveOperations.add(dataStoreManager.saveValueFuture(BREAK_SOUND_URI_KEY, settings.getBreakSoundUri()));
        } else {
            saveOperations.add(dataStoreManager.clearValueFuture(BREAK_SOUND_URI_KEY));
        }
        saveOperations.add(dataStoreManager.saveValueFuture(VIBRATION_ENABLED_KEY, settings.isVibrationEnabled()));

        return Futures.transform(Futures.allAsList(saveOperations), input -> null, directExecutor);
    }
}