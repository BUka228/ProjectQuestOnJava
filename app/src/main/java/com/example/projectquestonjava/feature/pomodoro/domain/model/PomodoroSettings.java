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
}