package com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels;

import androidx.annotation.Nullable;
import com.example.projectquestonjava.core.utils.RingtoneItem;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

public class SettingsUiState {
    // Getters
    @Getter
    private final PomodoroSettings currentSettings;
    @Getter
    private final List<RingtoneItem> systemRingtones;
    @Getter
    private final List<RingtoneItem> customRingtones;
    @Nullable
    private final String errorMessage;
    @Getter
    private final boolean showSuccess;
    @Getter
    private final boolean shouldNavigateBack;
    private final boolean isLoading;
    @Nullable
    private final String playingRingtoneUri; // URI рингтона, который сейчас проигрывается

    public SettingsUiState(
            PomodoroSettings currentSettings,
            List<RingtoneItem> systemRingtones,
            List<RingtoneItem> customRingtones,
            @Nullable String errorMessage,
            boolean showSuccess,
            boolean shouldNavigateBack,
            boolean isLoading,
            @Nullable String playingRingtoneUri
    ) {
        this.currentSettings = Objects.requireNonNullElseGet(currentSettings, PomodoroSettings::new);
        this.systemRingtones = systemRingtones != null ? systemRingtones : Collections.emptyList();
        this.customRingtones = customRingtones != null ? customRingtones : Collections.emptyList();
        this.errorMessage = errorMessage;
        this.showSuccess = showSuccess;
        this.shouldNavigateBack = shouldNavigateBack;
        this.isLoading = isLoading;
        this.playingRingtoneUri = playingRingtoneUri;
    }

    // Конструктор по умолчанию
    public SettingsUiState() {
        this(new PomodoroSettings(), Collections.emptyList(), Collections.emptyList(), null, false, false, false, null);
    }

    @Nullable public String getErrorMessage() { return errorMessage; }

    public boolean isLoading() { return isLoading; }
    @Nullable public String getPlayingRingtoneUri() { return playingRingtoneUri; }


    // "Copy" метод, который позволяет изменять только нужные поля
    public SettingsUiState copy(
            @Nullable PomodoroSettings currentSettings,
            @Nullable List<RingtoneItem> systemRingtones,
            @Nullable List<RingtoneItem> customRingtones,
            @Nullable String errorMessage, // Может быть null для сброса
            @Nullable Boolean showSuccess,
            @Nullable Boolean shouldNavigateBack,
            @Nullable Boolean isLoading,
            @Nullable String playingRingtoneUri, // Может быть null для сброса
            boolean explicitlyClearError, // Флаг для явной очистки ошибки
            boolean explicitlyClearSuccess // Флаг для явной очистки успеха
    ) {
        return new SettingsUiState(
                currentSettings != null ? currentSettings : this.currentSettings,
                systemRingtones != null ? systemRingtones : this.systemRingtones,
                customRingtones != null ? customRingtones : this.customRingtones,
                explicitlyClearError ? null : (errorMessage != null ? errorMessage : this.errorMessage), // !== null для String, чтобы не сбросить если передали null как "не менять"
                explicitlyClearSuccess ? false : (showSuccess != null ? showSuccess : this.showSuccess),
                shouldNavigateBack != null ? shouldNavigateBack : this.shouldNavigateBack,
                isLoading != null ? isLoading : this.isLoading,
                playingRingtoneUri != null ? playingRingtoneUri : this.playingRingtoneUri // !== null для String
        );
    }
    // Перегрузка для случаев, когда флаги очистки не нужны явно
    public SettingsUiState copy(
            @Nullable PomodoroSettings currentSettings,
            @Nullable List<RingtoneItem> systemRingtones,
            @Nullable List<RingtoneItem> customRingtones,
            @Nullable String errorMessage,
            @Nullable Boolean showSuccess,
            @Nullable Boolean shouldNavigateBack,
            @Nullable Boolean isLoading,
            @Nullable String playingRingtoneUri
    ) {
        return copy(currentSettings, systemRingtones, customRingtones, errorMessage, showSuccess,
                shouldNavigateBack, isLoading, playingRingtoneUri, errorMessage == null && this.errorMessage != null, showSuccess == null && this.showSuccess);
    }


    // Более удобные методы для частичного обновления
    public SettingsUiState withLoading(boolean isLoading) {
        return copy(null,null,null,null,null,null, isLoading, null, false,false);
    }
    public SettingsUiState withError(@Nullable String error) {
        return copy(null,null,null, error, null,null,null, null, error == null && this.errorMessage != null, false);
    }
    public SettingsUiState withSuccess(boolean success, @Nullable String messageIfSuccess) {
        return copy(null,null,null, success ? null : this.errorMessage, success, null,null, null, success, success ? false : this.showSuccess);
    }
    public SettingsUiState withNavigation(boolean navigate) {
        return copy(null,null,null,null,null,navigate,null, null, false,false);
    }
    public SettingsUiState withPlayingRingtone(@Nullable String uri) {
        return copy(null,null,null,null,null,null,null, uri, false,false);
    }
    public SettingsUiState withSettings(PomodoroSettings settings) {
        return copy(settings, null, null, null, null, null, null, null, false, false);
    }
    public SettingsUiState withCustomRingtones(List<RingtoneItem> ringtones) {
        return copy(null, null, ringtones, null, null, null, null, null, false, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettingsUiState that = (SettingsUiState) o;
        return showSuccess == that.showSuccess &&
                shouldNavigateBack == that.shouldNavigateBack &&
                isLoading == that.isLoading &&
                Objects.equals(currentSettings, that.currentSettings) &&
                Objects.equals(systemRingtones, that.systemRingtones) &&
                Objects.equals(customRingtones, that.customRingtones) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(playingRingtoneUri, that.playingRingtoneUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentSettings, systemRingtones, customRingtones, errorMessage,
                showSuccess, shouldNavigateBack, isLoading, playingRingtoneUri);
    }
}