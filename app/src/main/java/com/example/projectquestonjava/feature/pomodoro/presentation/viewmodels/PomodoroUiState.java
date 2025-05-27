package com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.projectquestonjava.feature.pomodoro.domain.logic.PomodoroCycleGenerator;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.pomodoro.domain.model.TimerState;

import java.util.Objects;

public final class PomodoroUiState {
    public final TimerState timerState;
    public final String formattedTime;
    public final float progress;
    @Nullable
    public final String errorMessage;
    public final SessionType currentPhaseType;
    public final int totalFocusSessionsInTask;
    public final int currentFocusSessionDisplayIndex;
    public final int estimatedHours;
    public final int estimatedMinutes;
    public final PomodoroSettings pomodoroSettings;
    public final boolean showLongTaskWarningDialog;
    public final boolean isCompletingTaskEarly;
    public final boolean isTimeSetupMode;

    public PomodoroUiState(
            TimerState timerState,
            String formattedTime,
            float progress,
            @Nullable String errorMessage,
            SessionType currentPhaseType,
            int totalFocusSessionsInTask,
            int currentFocusSessionDisplayIndex,
            int estimatedHours,
            int estimatedMinutes,
            PomodoroSettings pomodoroSettings,
            boolean showLongTaskWarningDialog,
            boolean isCompletingTaskEarly,
            boolean isTimeSetupMode
    ) {
        this.timerState = timerState != null ? timerState : TimerState.Idle.getInstance();
        this.formattedTime = formattedTime != null ? formattedTime : "00:00";
        this.progress = progress;
        this.errorMessage = errorMessage; // Может быть null
        this.currentPhaseType = currentPhaseType != null ? currentPhaseType : SessionType.FOCUS;
        this.totalFocusSessionsInTask = totalFocusSessionsInTask;
        this.currentFocusSessionDisplayIndex = currentFocusSessionDisplayIndex;
        this.estimatedHours = estimatedHours;
        this.estimatedMinutes = estimatedMinutes;
        this.pomodoroSettings = pomodoroSettings != null ? pomodoroSettings : new PomodoroSettings();
        this.showLongTaskWarningDialog = showLongTaskWarningDialog;
        this.isCompletingTaskEarly = isCompletingTaskEarly;
        this.isTimeSetupMode = isTimeSetupMode;
    }

    // Конструктор по умолчанию
    public PomodoroUiState() {
        this(
                TimerState.Idle.getInstance(),
                "00:00", // Начальное время для отображения
                0f,
                null,
                SessionType.FOCUS,
                0,
                0,
                0, // Начальные часы
                PomodoroCycleGenerator.FOCUS_DURATION_MINUTES, // Начальные минуты из константы
                new PomodoroSettings(),
                false,
                false,
                true // Изначально в режиме настройки времени
        );
    }

    // Метод для создания копии с изменениями (аналог copy в Kotlin data class)
    public PomodoroUiState copy(
            @Nullable TimerState timerState,
            @Nullable String formattedTime,
            @Nullable Float progress,
            @Nullable String errorMessage, // String уже nullable, аннотация для ясности
            @Nullable SessionType currentPhaseType,
            @Nullable Integer totalFocusSessionsInTask,
            @Nullable Integer currentFocusSessionDisplayIndex,
            @Nullable Integer estimatedHours,
            @Nullable Integer estimatedMinutes,
            @Nullable PomodoroSettings pomodoroSettings,
            @Nullable Boolean showLongTaskWarningDialog,
            @Nullable Boolean isCompletingTaskEarly,
            @Nullable Boolean isTimeSetupMode
    ) {
        return new PomodoroUiState(
                timerState != null ? timerState : this.timerState,
                formattedTime != null ? formattedTime : this.formattedTime,
                progress != null ? progress : this.progress,
                errorMessage, // Если передан null, поле станет null
                currentPhaseType != null ? currentPhaseType : this.currentPhaseType,
                totalFocusSessionsInTask != null ? totalFocusSessionsInTask : this.totalFocusSessionsInTask,
                currentFocusSessionDisplayIndex != null ? currentFocusSessionDisplayIndex : this.currentFocusSessionDisplayIndex,
                estimatedHours != null ? estimatedHours : this.estimatedHours,
                estimatedMinutes != null ? estimatedMinutes : this.estimatedMinutes,
                pomodoroSettings != null ? pomodoroSettings : this.pomodoroSettings,
                showLongTaskWarningDialog != null ? showLongTaskWarningDialog : this.showLongTaskWarningDialog,
                isCompletingTaskEarly != null ? isCompletingTaskEarly : this.isCompletingTaskEarly,
                isTimeSetupMode != null ? isTimeSetupMode : this.isTimeSetupMode
        );
    }


    public PomodoroUiState copy(
            @Nullable TimerState timerState,
            @Nullable String formattedTime,
            @Nullable Float progress,
            @Nullable String errorMessage,
            @Nullable SessionType currentPhaseType,
            @Nullable Integer totalFocusSessionsInTask,
            @Nullable Integer currentFocusSessionDisplayIndex,
            @Nullable Integer estimatedHours,
            @Nullable Integer estimatedMinutes,
            @Nullable PomodoroSettings pomodoroSettings,
            @Nullable Boolean showLongTaskWarningDialog,
            @Nullable Boolean isCompletingTaskEarly,
            @Nullable Boolean isTimeSetupMode,
            boolean clearErrorMessage,
            boolean clearSuccessMessage // Не используется здесь, но для сигнатуры
    ) {
        return new PomodoroUiState(
                timerState != null ? timerState : this.timerState,
                formattedTime != null ? formattedTime : this.formattedTime,
                progress != null ? progress : this.progress,
                clearErrorMessage ? null : (errorMessage != null ? errorMessage : this.errorMessage),
                currentPhaseType != null ? currentPhaseType : this.currentPhaseType,
                totalFocusSessionsInTask != null ? totalFocusSessionsInTask : this.totalFocusSessionsInTask,
                currentFocusSessionDisplayIndex != null ? currentFocusSessionDisplayIndex : this.currentFocusSessionDisplayIndex,
                estimatedHours != null ? estimatedHours : this.estimatedHours,
                estimatedMinutes != null ? estimatedMinutes : this.estimatedMinutes,
                pomodoroSettings != null ? pomodoroSettings : this.pomodoroSettings,
                showLongTaskWarningDialog != null ? showLongTaskWarningDialog : this.showLongTaskWarningDialog,
                isCompletingTaskEarly != null ? isCompletingTaskEarly : this.isCompletingTaskEarly,
                isTimeSetupMode != null ? isTimeSetupMode : this.isTimeSetupMode
        );
    }

    // Для удобства можно добавить перегруженные методы copy, если часто меняется только одно поле
    public PomodoroUiState withTimerState(TimerState timerState) {
        return copy(timerState, null, null, this.errorMessage, null, null, null, null, null, null, null, null, null);
    }

    public PomodoroUiState withFormattedTimeAndProgress(String formattedTime, float progress) {
        return copy(null, formattedTime, progress, this.errorMessage, null, null, null, null, null, null, null, null, null);
    }
    public PomodoroUiState withErrorMessage(@Nullable String errorMessage) {
        return copy(null, null, null, errorMessage, null, null, null, null, null, null, null, null, null);
    }
    // ... и так далее для других полей, если потребуется


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PomodoroUiState that = (PomodoroUiState) o;
        return Float.compare(that.progress, progress) == 0 &&
                totalFocusSessionsInTask == that.totalFocusSessionsInTask &&
                currentFocusSessionDisplayIndex == that.currentFocusSessionDisplayIndex &&
                estimatedHours == that.estimatedHours &&
                estimatedMinutes == that.estimatedMinutes &&
                showLongTaskWarningDialog == that.showLongTaskWarningDialog &&
                isCompletingTaskEarly == that.isCompletingTaskEarly &&
                isTimeSetupMode == that.isTimeSetupMode &&
                Objects.equals(timerState, that.timerState) &&
                Objects.equals(formattedTime, that.formattedTime) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                currentPhaseType == that.currentPhaseType &&
                Objects.equals(pomodoroSettings, that.pomodoroSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timerState, formattedTime, progress, errorMessage, currentPhaseType,
                totalFocusSessionsInTask, currentFocusSessionDisplayIndex, estimatedHours,
                estimatedMinutes, pomodoroSettings, showLongTaskWarningDialog,
                isCompletingTaskEarly, isTimeSetupMode);
    }

    @NonNull
    @Override
    public String toString() {
        return "PomodoroUiState{" +
                "timerState=" + timerState.getClass().getSimpleName() + // Для краткости
                ", formattedTime='" + formattedTime + '\'' +
                ", progress=" + progress +
                ", errorMessage='" + errorMessage + '\'' +
                ", currentPhaseType=" + currentPhaseType +
                ", totalFocusSessionsInTask=" + totalFocusSessionsInTask +
                ", currentFocusSessionDisplayIndex=" + currentFocusSessionDisplayIndex +
                ", estimatedHours=" + estimatedHours +
                ", estimatedMinutes=" + estimatedMinutes +
                ", pomodoroSettings=" + pomodoroSettings +
                ", showLongTaskWarningDialog=" + showLongTaskWarningDialog +
                ", isCompletingTaskEarly=" + isCompletingTaskEarly +
                ", isTimeSetupMode=" + isTimeSetupMode +
                '}';
    }
}
