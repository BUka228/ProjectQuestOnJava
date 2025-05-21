package com.example.projectquestonjava.feature.pomodoro.presentation.controllers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.example.projectquestonjava.app.MainActivity;
import com.example.projectquestonjava.feature.pomodoro.data.service.PomodoroTimerService;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PomodoroNotificationIntentProvider {

    private final Context context;

    @Inject
    public PomodoroNotificationIntentProvider(@ApplicationContext Context context) {
        this.context = context;
    }

    public PendingIntent createActionIntent(String action) {
        Intent intent = new Intent(context, PomodoroTimerService.class);
        intent.setAction(action);
        // Флаг IMMUTABLE важен для Android 12+
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(
                context,
                action.hashCode(), // Уникальный request code для каждого действия
                intent,
                flags
        );
    }

    public PendingIntent createOpenAppIntent() {
        Intent intent = new Intent(context, MainActivity.class);
        // Флаги для корректного возвращения в существующую Activity или создания новой
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(
                context,
                0, // Общий requestCode для открытия приложения
                intent,
                flags
        );
    }
}