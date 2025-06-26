package com.example.projectquestonjava.core.managers;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.utils.Logger;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List; // Для списка Actions

@Singleton
public class PomodoroNotificationManager {
    private static final String CHANNEL_ID = "pomodoro_channel";
    private static final String CHANNEL_NAME = "Pomodoro Таймер";

    private final Context context;
    private final NotificationManagerCompat notificationManagerCompat;
    private final Logger logger;

    @Inject
    public PomodoroNotificationManager(@ApplicationContext Context context, Logger logger) {
        this.context = context;
        this.notificationManagerCompat = NotificationManagerCompat.from(context);
        this.logger = logger;
    }

    public void createNotificationChannel() {
        // Проверка версии для создания канала
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            logger.debug("PomodoroNotificationManager", "Notification channel created: " + CHANNEL_ID);
        } else {
            logger.error("PomodoroNotificationManager", "NotificationManager service is null, channel not created.");
        }
    }

    public NotificationCompat.Builder buildNotification(
            String contentText,
            List<NotificationCompat.Action> actions,
            PendingIntent openAppIntent) { // openAppIntent теперь обязательный

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Pomodoro Таймер")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.timer)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(openAppIntent);

        if (actions != null) {
            for (NotificationCompat.Action action : actions) {
                builder.addAction(action);
            }
        }
        return builder;
    }

    public void updateNotification(int notificationId, NotificationCompat.Builder notificationBuilder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            logger.error("PomodoroNotificationManager", "Permission POST_NOTIFICATIONS not granted. Cannot show/update notification.");
            return;
        }
        try {
            notificationManagerCompat.notify(notificationId, notificationBuilder.build());
            logger.debug("PomodoroNotificationManager", "Notification updated/shown with ID: " + notificationId);
        } catch (SecurityException e) {
            // Это может произойти на некоторых устройствах, если сервис пытается показать уведомление
            // без должных разрешений или в неподходящий момент жизненного цикла.
            logger.error("PomodoroNotificationManager", "SecurityException while trying to notify.", e);
        }
    }

    public void cancelNotification(int notificationId) {
        notificationManagerCompat.cancel(notificationId);
        logger.debug("PomodoroNotificationManager", "Notification cancelled with ID: " + notificationId);
    }
}