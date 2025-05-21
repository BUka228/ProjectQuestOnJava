package com.example.projectquestonjava.core.managers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import com.example.projectquestonjava.core.utils.Logger;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SoundManager {
    private static final String TAG = "SoundManager";

    private final Context context;
    private final Logger logger;
    private MediaPlayer currentPlayer;
    private Future<?> currentPlayerJob; // Используем Future для управления задачей в ExecutorService
    private final ExecutorService soundExecutor = Executors.newSingleThreadExecutor(); // Однопоточный Executor для звуков
    private final Object lock = new Object();

    @Inject
    public SoundManager(@ApplicationContext Context context, Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    public void playSound(Uri uri) {
        synchronized (lock) {
            stopInternal();
            try {
                currentPlayer = new MediaPlayer();
                currentPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                );
                currentPlayer.setDataSource(context, uri);
                currentPlayer.prepare();
                currentPlayer.setOnCompletionListener(mp -> {
                    logger.debug(TAG, "playSound: MediaPlayer completed.");
                    releasePlayer(mp);
                    synchronized (lock) {
                        if (currentPlayer == mp) {
                            currentPlayer = null;
                        }
                    }
                });
                currentPlayer.setOnErrorListener((mp, what, extra) -> {
                    logger.error(TAG, "playSound: MediaPlayer error. What: " + what + ", Extra: " + extra);
                    releasePlayer(mp);
                    synchronized (lock) {
                        if (currentPlayer == mp) {
                            currentPlayer = null;
                        }
                    }
                    return true;
                });
                currentPlayer.start();
                logger.debug(TAG, "playSound: Started single playback.");
            } catch (IOException | IllegalStateException e) {
                logger.error(TAG, "playSound: Error setting up MediaPlayer", e);
                stopInternal();
            }
        }
    }

    public Future<?> playSoundLoop(Uri uri) {
        synchronized (lock) {
            stopInternal();
            currentPlayerJob = soundExecutor.submit(() -> {
                MediaPlayer localPlayer = null;
                try {
                    localPlayer = new MediaPlayer();
                    localPlayer.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                    );
                    localPlayer.setDataSource(context, uri);
                    localPlayer.prepare();
                    localPlayer.setLooping(true);

                    final MediaPlayer finalLocalPlayer = localPlayer; // Для доступа из другого потока
                    synchronized (lock) {
                        // Проверяем, не был ли job прерван, пока мы готовились
                        if (Thread.currentThread().isInterrupted()) {
                            logger.warn(TAG, "playSoundLoop: Loop cancelled before start. Releasing this instance.");
                            releasePlayer(finalLocalPlayer);
                            return;
                        }
                        currentPlayer = finalLocalPlayer; // Присваиваем только если все еще актуально
                    }

                    finalLocalPlayer.start();
                    logger.debug(TAG, "playSoundLoop: Started looping playback.");

                    // Держим поток активным, пока он не будет прерван
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(100); // Небольшая задержка, чтобы не загружать CPU
                        } catch (InterruptedException e) {
                            logger.info(TAG, "playSoundLoop: Loop interrupted.");
                            Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
                        }
                    }
                } catch (IOException | IllegalStateException e) {
                    logger.error(TAG, "playSoundLoop: Exception during setup or playback", e);
                } finally {
                    logger.debug(TAG, "playSoundLoop: Finalizing. Releasing localPlayer.");
                    releasePlayer(localPlayer); // localPlayer может быть null, если prepare не удался
                    synchronized (lock) {
                        if (currentPlayer == localPlayer) {
                            currentPlayer = null;
                            // currentPlayerJob будет null, если его прервали в stopInternal
                        }
                    }
                }
            });
            return currentPlayerJob;
        }
    }

    public void stop() {
        synchronized (lock) {
            stopInternal();
        }
    }

    private void stopInternal() {
        if (currentPlayerJob != null) {
            currentPlayerJob.cancel(true); // Прерываем поток, если он выполняется
            currentPlayerJob = null;
        }
        releasePlayer(currentPlayer);
        currentPlayer = null;
        logger.debug(TAG, "stopInternal: Playback stopped and player released.");
    }

    private void releasePlayer(MediaPlayer player) {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.reset();
                player.release();
                logger.debug(TAG, "MediaPlayer instance released.");
            } catch (IllegalStateException e) {
                logger.error(TAG, "IllegalStateException during player release: " + e.getMessage());
            } catch (Exception e) {
                logger.error(TAG, "Exception during player release: " + e.getMessage());
            }
        }
    }

    public void pause() {
        synchronized (lock) {
            try {
                if (currentPlayer != null && currentPlayer.isPlaying() && (currentPlayerJob == null || currentPlayerJob.isDone())) {
                    // Паузим, только если это не управляемый циклом плеер
                    currentPlayer.pause();
                }
            } catch (IllegalStateException e) {
                logger.error(TAG, "IllegalStateException on pause: " + e.getMessage());
            }
        }
    }

    public void resume() {
        synchronized (lock) {
            try {
                if (currentPlayer != null && (currentPlayerJob == null || currentPlayerJob.isDone())) {
                    currentPlayer.start();
                }
            } catch (IllegalStateException e) {
                logger.error(TAG, "IllegalStateException on resume: " + e.getMessage());
            }
        }
    }

    // Для корректного завершения ExecutorService
    public void shutdown() {
        logger.debug(TAG, "Shutting down SoundManager executor.");
        stop(); // Останавливаем текущее воспроизведение
        soundExecutor.shutdown();
    }
}