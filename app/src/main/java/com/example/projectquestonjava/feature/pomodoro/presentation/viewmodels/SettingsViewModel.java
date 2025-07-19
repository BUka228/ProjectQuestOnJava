package com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.RingtoneItem;
import com.example.projectquestonjava.core.utils.RingtoneUtil;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSettingsRepository;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors; // Для directExecutor

import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.inject.Inject;

@HiltViewModel
public class SettingsViewModel extends ViewModel {
    private static final String TAG = "SettingsViewModel";

    private final PomodoroSettingsRepository settingsRepository;
    private final RingtoneUtil ringtoneUtil;
    private final Executor ioExecutor;
    private final Logger logger;
    private final SnackbarManager snackbarManager;

    private final MutableLiveData<SettingsUiState> _uiState = new MutableLiveData<>(new SettingsUiState());
    public LiveData<SettingsUiState> uiStateLiveData = _uiState;

    // Observer для LiveData из PomodoroSettingsRepository
    private final androidx.lifecycle.Observer<PomodoroSettings> settingsRepoObserver;


    @Inject
    public SettingsViewModel(
            PomodoroSettingsRepository settingsRepository,
            RingtoneUtil ringtoneUtil,
            @IODispatcher Executor ioExecutor,
            Logger logger,
            SnackbarManager snackbarManager) {
        this.settingsRepository = settingsRepository;
        this.ringtoneUtil = ringtoneUtil;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.snackbarManager = snackbarManager;

        settingsRepoObserver = currentSettings -> {
            if (currentSettings == null) {
                logger.error(TAG, "Settings from repository are null.");
                updateUiState(s -> s.withError("Ошибка загрузки текущих настроек."));
                return;
            }
            // Загружаем рингтоны в фоновом потоке после получения настроек
            this.ioExecutor.execute(() -> {
                List<RingtoneItem> systemRingtones = this.ringtoneUtil.loadSystemRingtones();
                List<RingtoneItem> customRingtones = this.ringtoneUtil.loadCustomRingtones();
                updateUiState(s -> s.copy(
                        currentSettings, systemRingtones, customRingtones,
                        null, null, null, null, false, // isLoading = false
                        s.getPlayingRingtoneUri(), // Сохраняем текущий играющий рингтон
                        true, false // Явно очищаем ошибку, не трогаем флаг успеха
                ));
                logger.debug(TAG, "Initial settings and ringtones loaded: " + currentSettings);
            });
        };

        loadInitialData();
    }

    private void loadInitialData() {
        updateUiState(s -> s.withLoading(true));
        // Подписываемся на Flow настроек
        // settingsRepository.getSettingsFlow() возвращает LiveData<PomodoroSettings>
        settingsRepository.getSettingsFlow().observeForever(settingsRepoObserver);
    }

    private void updateUiState(UiStateUpdater updater) {
        SettingsUiState current = _uiState.getValue();
        // Используем postValue, так как обновление может быть вызвано из ioExecutor
        _uiState.postValue(updater.update(current != null ? current : new SettingsUiState()));
    }

    @FunctionalInterface
    interface UiStateUpdater {
        SettingsUiState update(SettingsUiState currentState);
    }

    public void addCustomRingtone(Uri uri) {
        updateUiState(s -> s.withLoading(true));
        ListenableFuture<RingtoneItem> addFuture = ringtoneUtil.addCustomRingtone(uri);
        Futures.addCallback(addFuture, new FutureCallback<RingtoneItem>() {
            @Override
            public void onSuccess(RingtoneItem newRingtone) {
                if (newRingtone != null) {
                    updateUiState(state -> {
                        List<RingtoneItem> updatedCustom = new ArrayList<>(state.getCustomRingtones());
                        updatedCustom.add(newRingtone);
                        return state.copy(null, null, updatedCustom, null,
                                true, null, null, false, null, false, true);
                    });
                    snackbarManager.showMessage("Рингтон добавлен");
                } else {
                    onFailure(new Exception("RingtoneUtil.addCustomRingtone вернул null"));
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to add custom ringtone", t);
                updateUiState(s -> s.copy(null,null,null, "Не удалось добавить рингтон", null, null, null, false, null, true, false));
            }
        }, ioExecutor);
    }

    public void removeCustomRingtone(RingtoneItem ringtone) {
        updateUiState(s -> s.withLoading(true));
        ListenableFuture<Boolean> removeFuture = ringtoneUtil.removeCustomRingtone(ringtone);
        Futures.addCallback(removeFuture, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean removed) {
                if (Boolean.TRUE.equals(removed)) {
                    updateUiState(state -> {
                        List<RingtoneItem> updatedCustom = new ArrayList<>(state.getCustomRingtones());
                        updatedCustom.remove(ringtone);

                        PomodoroSettings currentSettings = state.getCurrentSettings();
                        PomodoroSettings settingsToUpdate = currentSettings;
                        if (Objects.equals(currentSettings.getFocusSoundUri(), ringtone.uri())) {
                            settingsToUpdate = settingsToUpdate.copyFocusSoundUri(null);
                        }
                        if (Objects.equals(settingsToUpdate.getBreakSoundUri(), ringtone.uri())) {
                            settingsToUpdate = settingsToUpdate.copyBreakSoundUri(null);
                        }
                        return state.copy(settingsToUpdate, null, updatedCustom, null,
                                true, null, null, false, state.getPlayingRingtoneUri(), false, true);
                    });
                    snackbarManager.showMessage("Рингтон удален");
                } else {
                    onFailure(new Exception("Не удалось удалить файл рингтона"));
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to remove custom ringtone", t);
                updateUiState(s -> s.copy(null,null,null, "Ошибка удаления: " + t.getMessage(), null, null, null, false, null, true, false));
            }
        }, ioExecutor);
    }

    public void previewRingtone(String uriString) {
        SettingsUiState current = _uiState.getValue();
        if (current == null) return;

        if (Objects.equals(current.getPlayingRingtoneUri(), uriString)) {
            stopPreview();
        } else {
            stopPreview(); // Останавливаем предыдущий, если был
            try {
                ringtoneUtil.previewRingtone(uriString);
                updateUiState(s -> s.withPlayingRingtone(uriString));
            } catch (Exception e) {
                logger.error(TAG, "Failed to preview ringtone: " + uriString, e);
                updateUiState(s -> s.withError("Не удалось прослушать рингтон"));
            }
        }
    }

    public void stopPreview() {
        ringtoneUtil.stopPreview();
        updateUiState(s -> s.withPlayingRingtone(null));
    }

    public void updateSettings(PomodoroSettings settings) {
        // При простом обновлении настроек в UI, флаги error/success/navigate не меняем,
        // но флаг isLoading тоже не трогаем (он для фоновых операций)
        updateUiState(s -> s.copy(settings, null, null, null,
                null, null, null, null, s.getPlayingRingtoneUri(),
                true, true)); // Очищаем предыдущие ошибки/успехи
    }

    public void saveSettings() {
        SettingsUiState currentState = _uiState.getValue();
        if (currentState == null) return;
        PomodoroSettings settingsToSave = currentState.getCurrentSettings();
        logger.debug(TAG, "Attempting to save settings: " + settingsToSave);
        updateUiState(s -> s.withLoading(true).withError(null)); // Показываем загрузку, сбрасываем ошибку

        ListenableFuture<Void> future = settingsRepository.saveSettings(settingsToSave);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Settings saved successfully.");
                updateUiState(s -> s.withLoading(false).withSuccess(true, "Настройки сохранены").withNavigateToPomodoroScreen(true));
                // snackbarManager.showMessage("Настройки сохранены"); // Управляется через successMessage в UiState
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to save settings", t);
                updateUiState(s -> s.withLoading(false).withError("Ошибка сохранения: " + t.getMessage()));
            }
        }, MoreExecutors.directExecutor()); // Коллбэк на UI потоке (если MoreExecutors.directExecutor()) или ioExecutor
    }

    public void resetToDefaults() {
        PomodoroSettings defaultSettings = new PomodoroSettings();
        updateSettings(defaultSettings); // Обновляем UI
        // Сразу пытаемся сохранить (опционально, или пользователь должен нажать "Сохранить")
        // saveSettings();
        snackbarManager.showMessage("Настройки сброшены (нажмите 'Сохранить')");
    }

    public void onNavigatedBack() {
        updateUiState(s -> s.withNavigation(false).withSuccess(false, null));
    }

    public void onNavigatedToPomodoro() {
        updateUiState(s -> s.withNavigateToPomodoroScreen(false).withSuccess(false, null));
    }

    public void clearError() {
        updateUiState(s -> s.withError(null));
    }

    public void clearSuccessMessage() {
        updateUiState(s -> s.withSuccess(false, null));
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        // Отписываемся от LiveData, если использовали observeForever
        if (settingsRepoObserver != null) {
            settingsRepository.getSettingsFlow().removeObserver(settingsRepoObserver);
        }
        stopPreview(); // Останавливаем проигрывание при уничтожении ViewModel
        logger.debug(TAG, "SettingsViewModel cleared.");
    }
}