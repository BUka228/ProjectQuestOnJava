package com.example.projectquestonjava.feature.pomodoro.data.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.domain.model.InterruptedPhaseInfo;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroPhase;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.pomodoro.domain.model.TimerState;
import com.example.projectquestonjava.feature.pomodoro.presentation.controllers.PomodoroNotificationController;
import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.inject.Inject;

@AndroidEntryPoint
public class PomodoroTimerService extends Service {

    @Inject
    TimerEngine timerEngine;
    @Inject
    PomodoroNotificationController notificationController;
    @Inject
    Logger logger;

    private ExecutorService serviceActionExecutor; // Для обработки команд в onStartCommand
    private final PomodoroBinder binder = new PomodoroBinder();

    private boolean isForegroundService = false;
    private TimerState previousEngineStateForNotificationUpdates = TimerState.Idle.getInstance();
    private Intent lastReceivedIntentForRestore = null;

    // Observer для LiveData состояния таймера
    private Observer<TimerState> timerStateObserver;


    public class PomodoroBinder extends Binder {
        // Предоставляем LiveData напрямую из TimerEngine
        public LiveData<TimerState> getTimerStateLiveData() {
            return timerEngine.getTimerStateLiveData();
        }
        public LiveData<List<PomodoroPhase>> getPomodoroPhasesLiveData() {
            return timerEngine.getPomodoroPhasesLiveData();
        }
        public LiveData<Integer> getCurrentPhaseIndexLiveData() {
            return timerEngine.getCurrentPhaseIndexLiveData();
        }
        public InterruptedPhaseInfo getCurrentInterruptedPhaseInfoFromEngine() {
            return timerEngine.getCurrentInterruptedPhaseInfo();
        }
    }

    public static final String ACTION_START_POMODORO_CYCLE = "com.example.projectquestonjava.START_POMODORO_CYCLE";
    public static final String ACTION_PAUSE = "com.example.projectquestonjava.PAUSE";
    public static final String ACTION_RESUME = "com.example.projectquestonjava.RESUME";
    public static final String ACTION_STOP = "com.example.projectquestonjava.STOP";
    public static final String ACTION_CONFIRM = "com.example.projectquestonjava.CONFIRM";
    public static final String ACTION_SKIP_BREAK = "com.example.projectquestonjava.SKIP_BREAK";

    public static final String EXTRA_TASK_ID = "TASK_ID";
    public static final String EXTRA_USER_ID = "USER_ID";
    public static final String EXTRA_PHASES_LIST = "PHASES_LIST";

    private static final String TAG = "PomodoroTimerService";

    @Override
    public void onCreate() {
        super.onCreate();
        serviceActionExecutor = Executors.newSingleThreadExecutor();
        logger.debug(TAG, "Service onCreate");
        notificationController.createNotificationChannel();

        timerStateObserver = engineState -> {
            if (engineState == null) return;
            logger.debug(TAG, "Service Observed EngineState: " + engineState.getClass().getSimpleName() +
                    ", PrevNotifState: " + previousEngineStateForNotificationUpdates.getClass().getSimpleName());

            if (engineState instanceof TimerState.Running ||
                    engineState instanceof TimerState.Paused ||
                    engineState instanceof TimerState.WaitingForConfirmation) {
                ensureForegroundServiceStarted(engineState);
                // Обновляем уведомление если состояние изменилось ИЛИ если сервис только что стал foreground
                // ИЛИ если предыдущее состояние было Idle (первый запуск таймера)
                if (!engineState.equals(previousEngineStateForNotificationUpdates) ||
                        !isForegroundService /* стал foreground только что */ ||
                        previousEngineStateForNotificationUpdates instanceof TimerState.Idle) {
                    updateNotificationContent(engineState);
                }
            } else if (engineState instanceof TimerState.Idle) {
                stopForegroundServiceAndSelf();
            }
            previousEngineStateForNotificationUpdates = engineState;
        };
        // Подписываемся на LiveData из TimerEngine
        // Важно: TimerEngine должен быть инициализирован до этой подписки
        timerEngine.getTimerStateLiveData().observeForever(timerStateObserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;
        logger.info(TAG, "Service onStartCommand: action=" + action + ", startId=" + startId + ", flags=" + flags);

        if (intent != null) {
            // Создаем копию интента для восстановления
            // Важно: если интент содержит сложные Parcelable, убедитесь, что они корректно копируются
            lastReceivedIntentForRestore = new Intent(intent);
            if (intent.getExtras() != null) {
                lastReceivedIntentForRestore.putExtras(intent.getExtras());
            }
        }


        if (action == null && (flags & START_FLAG_REDELIVERY) != 0 && lastReceivedIntentForRestore != null) {
            logger.warn(TAG, "Service restarted by system. Re-delivering last intent. Current Engine State: " +
                    (timerEngine.getTimerStateLiveData().getValue() != null ? timerEngine.getTimerStateLiveData().getValue().getClass().getSimpleName() : "null"));
            // Просто возвращаем START_REDELIVER_INTENT, система повторно доставит lastReceivedIntentForRestore
            // onStartCommand будет вызван снова с этим интентом.
            // Логика проверки состояния движка и ensureForegroundServiceStarted должна быть в основном потоке onStartCommand.
        }


        // Вся обработка команд идет на Executor
        if (action != null) {
            final Intent finalIntent = intent; // Для использования в лямбде
            try {
                serviceActionExecutor.execute(() -> {
                    try {
                        switch (action) {
                            case ACTION_START_POMODORO_CYCLE:
                                long taskId = finalIntent.getLongExtra(EXTRA_TASK_ID, -1L);
                                int userId = finalIntent.getIntExtra(EXTRA_USER_ID, UserSessionManager.NO_USER_ID);
                                ArrayList<PomodoroPhase> phases = getParcelableArrayListExtraCompat(finalIntent);

                                if (taskId != -1L && userId != UserSessionManager.NO_USER_ID && phases != null && !phases.isEmpty()) {
                                    timerEngine.startPomodoroCycle(taskId, userId, phases);
                                } else {
                                    logger.error(TAG, "Invalid params for START_POMODORO_CYCLE. TaskId: " + taskId + ", UserId: " + userId + ", Phases null/empty: " + (phases == null || phases.isEmpty()));
                                    stopForegroundServiceAndSelf(); // Ошибка -> останавливаем сервис
                                }
                                break;
                            case ACTION_PAUSE:
                                timerEngine.pause();
                                break;
                            case ACTION_RESUME:
                                timerEngine.resume();
                                break;
                            case ACTION_STOP:
                                timerEngine.stopPomodoroCycle(true);
                                break;
                            case ACTION_CONFIRM:
                                timerEngine.confirmCompletionAndProceed();
                                break;
                            case ACTION_SKIP_BREAK:
                                timerEngine.skipCurrentBreak();
                                break;
                            default:
                                logger.warn(TAG, "Unknown action in onStartCommand: " + action);
                        }
                    } catch (Exception e) {
                        logger.error(TAG, "Error processing action: " + action + " in executor", e);
                    }
                });
            } catch (RejectedExecutionException e) {
                logger.error(TAG, "Failed to execute action " + action + " on serviceActionExecutor (shutdown or saturated)", e);
                // Если Executor уже остановлен, можно попытаться остановить сервис
                stopForegroundServiceAndSelf();
            }
        } else if (intent == null && flags == 0 && startId > 0) { // Перезапуск через START_STICKY
            TimerState engineCurrentState = timerEngine.getTimerStateLiveData().getValue();
            if (engineCurrentState != null && !(engineCurrentState instanceof TimerState.Idle)) {
                logger.info(TAG,"Service restarted (STICKY) and Engine was not Idle (" + engineCurrentState.getClass().getSimpleName() + "). Ensuring foreground.");
                ensureForegroundServiceStarted(engineCurrentState);
            } else {
                logger.info(TAG, "Service restarted (STICKY) but Engine is Idle or null. Stopping service.");
                stopForegroundServiceAndSelf();
            }
        }

        return START_REDELIVER_INTENT;
    }

    private void ensureForegroundServiceStarted(TimerState engineState) {
        // Этот метод может вызываться из разных потоков (например, из observeForever или onStartCommand),
        // поэтому startForeground должен быть безопасен.
        // PomodoroNotificationController.startForeground уже должен это учитывать.
        if (!isForegroundService) {
            try {
                notificationController.startForeground(
                        this,
                        getNotificationText(engineState),
                        getNotificationActionsForState(engineState)
                );
                isForegroundService = true;
                logger.info(TAG, "Service started in foreground. State: " + engineState.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error(TAG, "Error starting foreground service", e);
                // Аварийная остановка таймера, если не удалось запустить foreground
                serviceActionExecutor.execute(() -> timerEngine.stopPomodoroCycle(false));
            }
        }
    }

    private void updateNotificationContent(TimerState state) {
        if (isForegroundService) {
            notificationController.updateNotification(
                    getNotificationText(state),
                    getNotificationActionsForState(state)
            );
        }
    }

    private String getNotificationText(TimerState state) {
        PomodoroPhase currentPhase = timerEngine.getCurrentPhaseLiveData().getValue();
        SessionType phaseType = phaseFromStateOrEngine(state, currentPhase);
        String phaseTypeString = getPhaseTypeString(phaseType);

        if (state instanceof TimerState.Running) {
            return phaseTypeString + ": " + formatTime(((TimerState.Running) state).getRemainingSeconds());
        } else if (state instanceof TimerState.Paused) {
            return "Пауза (" + phaseTypeString + "): " + formatTime(((TimerState.Paused) state).getRemainingSeconds());
        } else if (state instanceof TimerState.WaitingForConfirmation) {
            return phaseTypeString + " завершён";
        }
        return "Pomodoro готов";
    }

    private List<NotificationCompat.Action> getNotificationActionsForState(TimerState state) {
        PomodoroPhase currentPhase = timerEngine.getCurrentPhaseLiveData().getValue();
        List<NotificationCompat.Action> actions = new ArrayList<>();

        if (state instanceof TimerState.Running) {
            if (currentPhase != null && currentPhase.isBreak()) {
                actions.add(notificationController.createAction(ACTION_SKIP_BREAK, R.drawable.arrow_forward, "Фокус"));
            }
            actions.add(notificationController.createAction(ACTION_PAUSE, R.drawable.pause, "Пауза"));
            actions.add(notificationController.createAction(ACTION_STOP, R.drawable.stop, "Стоп"));
        } else if (state instanceof TimerState.Paused) {
            if (currentPhase != null && currentPhase.isBreak()) {
                actions.add(notificationController.createAction(ACTION_SKIP_BREAK, R.drawable.arrow_forward, "Фокус"));
            }
            actions.add(notificationController.createAction(ACTION_RESUME, R.drawable.resume, "Возобновить"));
            actions.add(notificationController.createAction(ACTION_STOP, R.drawable.stop, "Стоп"));
        } else if (state instanceof TimerState.WaitingForConfirmation) {
            actions.add(notificationController.createAction(ACTION_CONFIRM, R.drawable.check, "Подтвердить"));
            if (((TimerState.WaitingForConfirmation) state).getType().isFocus()) {
                actions.add(notificationController.createAction(ACTION_SKIP_BREAK, R.drawable.arrow_forward, "Пропустить"));
            }
            actions.add(notificationController.createAction(ACTION_STOP, R.drawable.stop, "Стоп"));
        }
        return actions;
    }

    private SessionType phaseFromStateOrEngine(TimerState state, @Nullable PomodoroPhase enginePhase) {
        if (state instanceof TimerState.Running) return ((TimerState.Running) state).getType();
        if (state instanceof TimerState.Paused) return ((TimerState.Paused) state).getType();
        if (state instanceof TimerState.WaitingForConfirmation) return ((TimerState.WaitingForConfirmation) state).getType();
        return (enginePhase != null) ? enginePhase.getType() : SessionType.FOCUS;
    }

    private String getPhaseTypeString(@Nullable SessionType phaseType) {
        if (phaseType == null) return "Фаза";
        return switch (phaseType) {
            case FOCUS -> "Фокус";
            case SHORT_BREAK -> "Короткий перерыв";
            case LONG_BREAK -> "Длинный перерыв";
        };
    }

    private void stopForegroundServiceAndSelf() {
        logger.info(TAG, "Stopping foreground service and self.");
        isForegroundService = false;
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds);
    }

    @Override
    public void onDestroy() {
        logger.info(TAG, "Service onDestroy. Removing observer, shutting down executor.");
        if (timerStateObserver != null && timerEngine != null && timerEngine.getTimerStateLiveData() != null) {
            timerEngine.getTimerStateLiveData().removeObserver(timerStateObserver);
        }
        if (serviceActionExecutor != null && !serviceActionExecutor.isShutdown()) {
            serviceActionExecutor.shutdownNow();
        }
        // Если TimerEngine не Singleton и должен останавливаться с сервисом:
        // if (timerEngine != null) {
        //     timerEngine.shutdown();
        // }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        logger.debug(TAG, "Service onBind");
        return binder;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Parcelable> ArrayList<T> getParcelableArrayListExtraCompat(
            @NonNull Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableArrayListExtra(PomodoroTimerService.EXTRA_PHASES_LIST, (Class<T>) PomodoroPhase.class);
        } else {
            try {
                @SuppressWarnings("deprecation")
                ArrayList<Parcelable> parcelableArrayList = intent.getParcelableArrayListExtra(PomodoroTimerService.EXTRA_PHASES_LIST);
                if (parcelableArrayList == null) {
                    return null;
                }
                ArrayList<T> typedList = new ArrayList<>();
                for (Parcelable p : parcelableArrayList) {
                    if (p instanceof PomodoroPhase) {
                        typedList.add(((Class<T>) PomodoroPhase.class).cast(p));
                    } else {
                        return null;
                    }
                }
                return typedList;
            } catch (Exception e) {
                return null;
            }
        }
    }
}