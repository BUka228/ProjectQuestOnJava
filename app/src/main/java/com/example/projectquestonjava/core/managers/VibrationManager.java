package com.example.projectquestonjava.core.managers;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VibrationManager {
    private final Context context;
    private Future<?> vibrationJob;
    private final ExecutorService vibrationExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public VibrationManager(@ApplicationContext Context context) {
        this.context = context;
    }

    private Vibrator getVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            @SuppressWarnings("deprecation")
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            return vibrator;
        }
    }

    public void startVibrationLoop(long vibrationDuration, long delayBetweenVibrations) {
        stopVibrationLoop(); // Останавливаем предыдущий цикл, если он был
        vibrationJob = vibrationExecutor.submit(() -> {
            Vibrator vibrator = getVibrator();
            if (vibrator == null || !vibrator.hasVibrator()) {
                return; // Нет вибратора или возможности вибрировать
            }
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE));
                    Thread.sleep(delayBetweenVibrations);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
            }
        });
    }
    // Перегруженный метод с значениями по умолчанию
    public void startVibrationLoop() {
        startVibrationLoop(500L, 2000L);
    }


    public void stopVibrationLoop() {
        if (vibrationJob != null) {
            vibrationJob.cancel(true); // true для прерывания потока
            vibrationJob = null;
        }
    }

    public void vibrateOnce(long vibrationDuration) {
        Vibrator vibrator = getVibrator();
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
    // Перегруженный метод с значением по умолчанию
    public void vibrateOnce() {
        vibrateOnce(500L);
    }


    public void vibratePattern(long[] pattern, int repeat) {
        Vibrator vibrator = getVibrator();
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat));
        }
    }

    // Для корректного завершения ExecutorService
    public void shutdown() {
        stopVibrationLoop();
        vibrationExecutor.shutdown();
    }
}