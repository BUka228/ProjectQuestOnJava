package com.example.projectquestonjava.feature.pomodoro.domain.model;

import androidx.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PomodoroSettings {
    private int workDurationMinutes = 25;
    private int breakDurationMinutes = 5;
    @Nullable
    private String focusSoundUri;
    @Nullable
    private String breakSoundUri;
    private boolean vibrationEnabled = true;

    public PomodoroSettings copyWorkDurationMinutes(int workDurationMinutes) {
        return new PomodoroSettings(workDurationMinutes, this.breakDurationMinutes, this.focusSoundUri, this.breakSoundUri, this.vibrationEnabled);
    }
    public PomodoroSettings copyBreakDurationMinutes(int breakDurationMinutes) {
        return new PomodoroSettings(this.workDurationMinutes, breakDurationMinutes, this.focusSoundUri, this.breakSoundUri, this.vibrationEnabled);
    }
    public PomodoroSettings copyFocusSoundUri(@Nullable String focusSoundUri) {
        return new PomodoroSettings(this.workDurationMinutes, this.breakDurationMinutes, focusSoundUri, this.breakSoundUri, this.vibrationEnabled);
    }
    public PomodoroSettings copyBreakSoundUri(@Nullable String breakSoundUri) {
        return new PomodoroSettings(this.workDurationMinutes, this.breakDurationMinutes, this.focusSoundUri, breakSoundUri, this.vibrationEnabled);
    }
    public PomodoroSettings copyVibrationEnabled(boolean vibrationEnabled) {
        return new PomodoroSettings(this.workDurationMinutes, this.breakDurationMinutes, this.focusSoundUri, this.breakSoundUri, vibrationEnabled);
    }
}