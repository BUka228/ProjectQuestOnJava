package com.example.projectquestonjava.feature.pomodoro.presentation.controllers;

import android.app.PendingIntent;
import android.app.Service;
import androidx.core.app.NotificationCompat;
import com.example.projectquestonjava.core.managers.PomodoroNotificationManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List; // Для NotificationCompat.Action

@Singleton
public class PomodoroNotificationController {

    public static final int NOTIFICATION_ID = 1;

    private final PomodoroNotificationManager notificationManager;
    private final PomodoroNotificationIntentProvider intentProvider;

    @Inject
    public PomodoroNotificationController(
            PomodoroNotificationManager notificationManager,
            PomodoroNotificationIntentProvider intentProvider) {
        this.notificationManager = notificationManager;
        this.intentProvider = intentProvider;
    }

    public void createNotificationChannel() {
        notificationManager.createNotificationChannel();
    }

    public void startForeground(Service service, String contentText, List<NotificationCompat.Action> actions) {
        PendingIntent openAppIntent = intentProvider.createOpenAppIntent();
        NotificationCompat.Builder notificationBuilder = notificationManager.buildNotification(contentText, actions, openAppIntent);
        service.startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    public void updateNotification(String contentText, List<NotificationCompat.Action> actions) {
        PendingIntent openAppIntent = intentProvider.createOpenAppIntent();
        NotificationCompat.Builder notificationBuilder = notificationManager.buildNotification(contentText, actions, openAppIntent);
        notificationManager.updateNotification(NOTIFICATION_ID, notificationBuilder);
    }


    public NotificationCompat.Action createAction(String actionString, int iconResId, String title) {
        PendingIntent pendingIntent = intentProvider.createActionIntent(actionString);
        return new NotificationCompat.Action.Builder(iconResId, title, pendingIntent).build();
    }

    public void cancelNotification() {
        notificationManager.cancelNotification(NOTIFICATION_ID);
    }
}