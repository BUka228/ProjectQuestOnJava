package com.example.projectquestonjava.feature.pomodoro.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.google.common.util.concurrent.ListenableFuture; // Для saveSettings

public interface PomodoroSettingsRepository {
    LiveData<PomodoroSettings> getSettingsFlow();
    ListenableFuture<PomodoroSettings> getSettings();
    ListenableFuture<Void> saveSettings(PomodoroSettings settings);
}