package com.example.projectquestonjava.feature.pomodoro.data.managers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.data.service.PomodoroTimerService;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroPhase;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class PomodoroManager {
    private static final String TAG = "PomodoroManager";
    private final Context context;
    private final Logger logger;

    @Inject
    public PomodoroManager(@ApplicationContext Context context, Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    public ComponentName startPomodoroCycle(long taskId, int userId, List<PomodoroPhase> phases) {
        logger.debug(TAG, "Sending START_POMODORO_CYCLE intent for taskId=" + taskId + ", userId=" + userId + ", with " + phases.size() + " phases");
        try {
            ArrayList<PomodoroPhase> phasesArrayList = new ArrayList<>(phases); // Конвертируем в ArrayList

            Intent intent = new Intent(context, PomodoroTimerService.class);
            intent.setAction(PomodoroTimerService.ACTION_START_POMODORO_CYCLE);
            intent.putExtra(PomodoroTimerService.EXTRA_TASK_ID, taskId);
            intent.putExtra(PomodoroTimerService.EXTRA_USER_ID, userId);
            intent.putParcelableArrayListExtra(PomodoroTimerService.EXTRA_PHASES_LIST, phasesArrayList);

            return context.startService(intent);
        } catch (Exception e) {
            logger.error(TAG, "Failed to start pomodoro cycle intent: " + e.getMessage(), e);
            return null; // Возвращаем null в случае ошибки
        }
    }

    public void pauseTimer() {
        logger.debug(TAG, "Sending PAUSE intent");
        Intent intent = new Intent(context, PomodoroTimerService.class);
        intent.setAction(PomodoroTimerService.ACTION_PAUSE);
        context.startService(intent);
    }

    public void confirmTimerCompletion() {
        logger.debug(TAG, "Sending CONFIRM intent");
        Intent intent = new Intent(context, PomodoroTimerService.class);
        intent.setAction(PomodoroTimerService.ACTION_CONFIRM);
        context.startService(intent);
    }

    public void resumeTimer() {
        logger.debug(TAG, "Sending RESUME intent");
        Intent intent = new Intent(context, PomodoroTimerService.class);
        intent.setAction(PomodoroTimerService.ACTION_RESUME);
        context.startService(intent);
    }

    public void stopTimer() {
        logger.debug(TAG, "Sending STOP intent");
        Intent intent = new Intent(context, PomodoroTimerService.class);
        intent.setAction(PomodoroTimerService.ACTION_STOP);
        context.startService(intent);
    }

    public void skipBreak() {
        logger.debug(TAG, "Sending SKIP_BREAK intent");
        Intent intent = new Intent(context, PomodoroTimerService.class);
        intent.setAction(PomodoroTimerService.ACTION_SKIP_BREAK);
        context.startService(intent);
    }

    public String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds);
    }
}