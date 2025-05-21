package com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels;

import com.example.projectquestonjava.core.utils.RingtoneItem;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import androidx.annotation.Nullable;


@Data
@NoArgsConstructor
public class SettingsUiState {
    private PomodoroSettings currentSettings = new PomodoroSettings();
    private List<RingtoneItem> systemRingtones = Collections.emptyList();
    private List<RingtoneItem> customRingtones = Collections.emptyList();
    @Nullable
    private String errorMessage;
    private boolean showSuccess = false; // Для Snackbar/Toast
    private boolean shouldNavigateBack = false; // Для навигации

    public SettingsUiState(PomodoroSettings currentSettings, List<RingtoneItem> systemRingtones, List<RingtoneItem> customRingtones, @Nullable String errorMessage, boolean showSuccess, boolean shouldNavigateBack) {
        this.currentSettings = currentSettings;
        this.systemRingtones = systemRingtones;
        this.customRingtones = customRingtones;
        this.errorMessage = errorMessage;
        this.showSuccess = showSuccess;
        this.shouldNavigateBack = shouldNavigateBack;
    }

    public SettingsUiState copy(
            @Nullable PomodoroSettings currentSettings,
            @Nullable List<RingtoneItem> systemRingtones,
            @Nullable List<RingtoneItem> customRingtones,
            @Nullable String errorMessage, // String может быть null
            @Nullable Boolean showSuccess,
            @Nullable Boolean shouldNavigateBack
    ) {
        return new SettingsUiState(
                currentSettings != null ? currentSettings : this.currentSettings,
                systemRingtones != null ? systemRingtones : this.systemRingtones,
                customRingtones != null ? customRingtones : this.customRingtones,
                errorMessage, // Просто передаем
                showSuccess != null ? showSuccess : this.showSuccess,
                shouldNavigateBack != null ? shouldNavigateBack : this.shouldNavigateBack
        );
    }
}