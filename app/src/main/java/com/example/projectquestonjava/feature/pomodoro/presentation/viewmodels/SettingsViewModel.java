package com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.RingtoneItem;
import com.example.projectquestonjava.core.utils.RingtoneUtil;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSettingsRepository;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {
    private static final String TAG = "SettingsViewModel";

    private final PomodoroSettingsRepository settingsRepository;
    private final RingtoneUtil ringtoneUtil;
    private final Executor ioExecutor;
    private final Logger logger;

    private final MutableLiveData<SettingsUiState> _uiState = new MutableLiveData<>(new SettingsUiState());
    public LiveData<SettingsUiState> uiStateLiveData = _uiState;

    @Inject
    public SettingsViewModel(
            PomodoroSettingsRepository settingsRepository,
            RingtoneUtil ringtoneUtil,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.settingsRepository = settingsRepository;
        this.ringtoneUtil = ringtoneUtil;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        loadInitialData();
    }

    private void loadInitialData() {
        // Загружаем настройки и рингтоны
        // Используем MediatorLiveData для объединения LiveData от настроек и синхронной загрузки рингтонов
        MediatorLiveData<PomodoroSettings> settingsSource = (MediatorLiveData<PomodoroSettings>) settingsRepository.getSettingsFlow();

        _uiState.addSource(settingsSource, currentSettings -> {
            if (currentSettings == null) {
                logger.error(TAG, "Failed to load initial settings, LiveData returned null.");
                updateUiState(state -> state.copy(null, null, null, "Ошибка загрузки настроек", null, null));
                return;
            }
            // Загружаем рингтоны в фоновом потоке
            ioExecutor.execute(() -> {
                List<RingtoneItem> systemRingtones = ringtoneUtil.loadSystemRingtones();
                List<RingtoneItem> customRingtones = ringtoneUtil.loadCustomRingtones();
                SettingsUiState newState = new SettingsUiState(currentSettings, systemRingtones, customRingtones, null, false, false);
                _uiState.postValue(newState);
                logger.debug(TAG, "Initial settings and ringtones loaded: " + currentSettings);
            });
        });
    }

    private void updateUiState(UiStateUpdater updater) {
        SettingsUiState currentState = _uiState.getValue();
        if (currentState != null) {
            _uiState.postValue(updater.update(currentState));
        } else {
            _uiState.postValue(updater.update(new SettingsUiState())); // На случай если currentState был null
        }
    }

    @FunctionalInterface
    interface UiStateUpdater {
        SettingsUiState update(SettingsUiState currentState);
    }

    public void addCustomRingtone(Uri uri) {
        ioExecutor.execute(() -> {
            try {
                RingtoneItem newRingtone = ringtoneUtil.addCustomRingtone(uri); // addCustomRingtone теперь suspend
                if (newRingtone != null) {
                    updateUiState(state -> {
                        List<RingtoneItem> updatedCustomRingtones = new ArrayList<>(state.getCustomRingtones());
                        updatedCustomRingtones.add(newRingtone);
                        return state.copy(null, null, updatedCustomRingtones, null, null, null);
                    });
                }
            } catch (Exception e) {
                logger.error(TAG, "Failed to add custom ringtone", e);
                updateUiState(state -> state.copy(null, null, null, "Не удалось добавить рингтон", null, null));
            }
        });
    }

    public void removeCustomRingtone(RingtoneItem ringtone) {
        ioExecutor.execute(() -> {
            try {
                if (ringtoneUtil.removeCustomRingtone(ringtone)) { // removeCustomRingtone теперь suspend
                    updateUiState(state -> {
                        List<RingtoneItem> updatedCustomRingtones = new ArrayList<>(state.getCustomRingtones());
                        updatedCustomRingtones.remove(ringtone);

                        PomodoroSettings currentSettings = state.getCurrentSettings();
                        PomodoroSettings settingsToUpdate = currentSettings;
                        if (Objects.equals(currentSettings.getFocusSoundUri(), ringtone.uri())) {
                            settingsToUpdate = new PomodoroSettings(settingsToUpdate.getWorkDurationMinutes(), settingsToUpdate.getBreakDurationMinutes(), null, settingsToUpdate.getBreakSoundUri(), settingsToUpdate.isVibrationEnabled());
                        }
                        if (Objects.equals(currentSettings.getBreakSoundUri(), ringtone.uri())) {
                            settingsToUpdate = new PomodoroSettings(settingsToUpdate.getWorkDurationMinutes(), settingsToUpdate.getBreakDurationMinutes(), settingsToUpdate.getFocusSoundUri(), null, settingsToUpdate.isVibrationEnabled());
                        }
                        return state.copy(settingsToUpdate, null, updatedCustomRingtones, null, null, null);
                    });
                } else {
                    updateUiState(state -> state.copy(null, null, null, "Не удалось удалить рингтон", null, null));
                }
            } catch (Exception e) {
                logger.error(TAG, "Failed to remove custom ringtone", e);
                updateUiState(state -> state.copy(null, null, null, "Ошибка при удалении рингтона", null, null));
            }
        });
    }

    public void previewRingtone(String uriString) {
        try {
            ringtoneUtil.previewRingtone(uriString);
        } catch (Exception e) {
            logger.error(TAG, "Failed to preview ringtone: " + uriString, e);
            updateUiState(state -> state.copy(null, null, null, "Не удалось прослушать рингтон", null, null));
        }
    }

    public void stopPreview() {
        ringtoneUtil.stopPreview();
    }

    public void updateSettings(PomodoroSettings settings) {
        updateUiState(state -> state.copy(settings, null, null, null, false, false));
    }

    public void saveSettings() {
        SettingsUiState currentState = _uiState.getValue();
        if (currentState == null) return;

        PomodoroSettings settingsToSave = currentState.getCurrentSettings();
        logger.debug(TAG, "Attempting to save settings: " + settingsToSave);

        ListenableFuture<Void> future = settingsRepository.saveSettings(settingsToSave);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Settings saved successfully.");
                updateUiState(state -> state.copy(null, null, null, null, true, true));
            }

            @Override
            public void onFailure(Throwable t) {
                logger.error(TAG, "Failed to save settings", t);
                updateUiState(state -> state.copy(null, null, null, "Ошибка сохранения: " + t.getMessage(), false, false));
            }
        }, MoreExecutors.directExecutor()); // Коллбэк на том же потоке, что и Future
    }

    public void onNavigatedBack() {
        updateUiState(state -> state.copy(null, null, null, null, null, false));
    }

    public void clearError() {
        updateUiState(state -> state.copy(null, null, null, null, null, null));
    }
}