package com.example.projectquestonjava.feature.pomodoro.data.service;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.SoundManager;
import com.example.projectquestonjava.core.managers.VibrationManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.data.managers.PomodoroSessionManager;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.example.projectquestonjava.feature.pomodoro.domain.model.InterruptedPhaseInfo;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroPhase;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.pomodoro.domain.model.TimerState;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSettingsRepository;
import com.example.projectquestonjava.feature.pomodoro.domain.usecases.CompletePomodoroSessionUseCase;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TimerEngine {
    private static final String TAG = "TimerEngine";

    private final SoundManager soundManager;
    private final VibrationManager vibrationManager;
    private final CompletePomodoroSessionUseCase completePomodoroSessionUseCase;
    private final PomodoroSettingsRepository settingsRepository;
    private final PomodoroSessionManager pomodoroSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final ExecutorService engineExecutor;
    private final ExecutorService settingsExecutor;
    private Logger logger = null;

    private final MutableLiveData<TimerState> _timerStateLiveData = new MutableLiveData<>(TimerState.Idle.getInstance());
    public LiveData<TimerState> getTimerStateLiveData() {
        return _timerStateLiveData;
    }

    private final MutableLiveData<List<PomodoroPhase>> _pomodoroPhasesLiveData = new MutableLiveData<>(Collections.emptyList());
    public LiveData<List<PomodoroPhase>> getPomodoroPhasesLiveData() {
        return _pomodoroPhasesLiveData;
    }

    private final MutableLiveData<Integer> _currentPhaseIndexLiveData = new MutableLiveData<>(-1);
    public LiveData<Integer> getCurrentPhaseIndexLiveData() {
        return _currentPhaseIndexLiveData;
    }

    private final MediatorLiveData<PomodoroPhase> _currentPhaseLiveData = new MediatorLiveData<>();
    public LiveData<PomodoroPhase> getCurrentPhaseLiveData() {
        return _currentPhaseLiveData;
    }

    private volatile Long currentTaskIdForCycle = null;
    private volatile Integer currentUserIdForCycle = null;
    private volatile Long currentDbSessionIdForPhase = null;
    private volatile LocalDateTime currentPhaseStartTime = null; // UTC
    private final AtomicInteger accumulatedInterruptionsInCurrentPhase = new AtomicInteger(0);

    private volatile Future<?> activeTimerJob = null;
    private final Object timerJobLock = new Object();
    private final AtomicReference<PomodoroSettings> currentSettingsRef = new AtomicReference<>(new PomodoroSettings());
    private List<PomodoroPhase> originalPhasesForCurrentCycle = Collections.emptyList();


    @Inject
    public TimerEngine(
            SoundManager soundManager,
            VibrationManager vibrationManager,
            CompletePomodoroSessionUseCase completePomodoroSessionUseCase,
            PomodoroSettingsRepository settingsRepository,
            PomodoroSessionManager pomodoroSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.soundManager = soundManager;
        this.vibrationManager = vibrationManager;
        this.completePomodoroSessionUseCase = completePomodoroSessionUseCase;
        this.settingsRepository = settingsRepository;
        this.pomodoroSessionManager = pomodoroSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.engineExecutor = (ExecutorService) ioExecutor;
        this.settingsExecutor = Executors.newSingleThreadExecutor();
        this.logger = logger;

        _currentPhaseLiveData.addSource(_pomodoroPhasesLiveData, phases -> updateCurrentPhaseMediator());
        _currentPhaseLiveData.addSource(_currentPhaseIndexLiveData, index -> updateCurrentPhaseMediator());

        loadInitialSettings();
    }

    private void updateCurrentPhaseMediator() {
        List<PomodoroPhase> phases = _pomodoroPhasesLiveData.getValue();
        Integer index = _currentPhaseIndexLiveData.getValue();
        if (phases != null && index != null && index >= 0 && index < phases.size()) {
            _currentPhaseLiveData.setValue(phases.get(index));
        } else {
            _currentPhaseLiveData.setValue(null);
        }
    }

    private void loadInitialSettings() {
        settingsExecutor.execute(() -> {
            try {
                ListenableFuture<PomodoroSettings> settingsFuture = settingsRepository.getSettings();
                Futures.addCallback(settingsFuture, new FutureCallback<PomodoroSettings>() {
                    @Override
                    public void onSuccess(@Nullable PomodoroSettings settings) {
                        if (settings == null) {
                            logger.error(TAG, "Initial settings loaded as null. Using defaults.");
                            currentSettingsRef.set(new PomodoroSettings());
                        } else {
                            currentSettingsRef.set(settings);
                            logger.debug(TAG, "Initial settings loaded: " + settings);
                        }
                        TimerState currentTimerState = _timerStateLiveData.getValue();
                        List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();
                        if (currentTimerState instanceof TimerState.Idle &&
                                (currentPhases == null || currentPhases.isEmpty())) {
                            _timerStateLiveData.postValue(TimerState.Idle.getInstance());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        logger.error(TAG, "Failed to load initial settings due to Future failure. Using defaults.", t);
                        currentSettingsRef.set(new PomodoroSettings());
                        TimerState currentTimerState = _timerStateLiveData.getValue();
                        List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();
                        if (currentTimerState instanceof TimerState.Idle &&
                                (currentPhases == null || currentPhases.isEmpty())) {
                            _timerStateLiveData.postValue(TimerState.Idle.getInstance());
                        }
                    }
                }, MoreExecutors.directExecutor());

            } catch (Exception e) {
                logger.error(TAG, "Unexpected error during initial settings load trigger", e);
                currentSettingsRef.set(new PomodoroSettings());
            }
        });
        settingsRepository.getSettingsFlow().observeForever(settingsObserver);
    }

    public void startPomodoroCycle(long taskId, int userId, List<PomodoroPhase> phases) {
        final List<PomodoroPhase> finalPhases = new ArrayList<>(phases);
        this.originalPhasesForCurrentCycle = new ArrayList<>(finalPhases);

        engineExecutor.execute(() -> {
            logger.info(TAG, "CMD: Start Cycle. Task: " + taskId + ", User: " + userId + ", Phases: " + finalPhases.size());
            synchronized (timerJobLock) {
                cancelActiveTimerJobInternal();
            }
            stopCurrentOperationAndSound();

            if (finalPhases.isEmpty()) {
                logger.warn(TAG, "Cannot start cycle: phase list is empty.");
                setIdleStateAndResetCycleInternals(true);
                return;
            }

            _pomodoroPhasesLiveData.postValue(finalPhases);
            _currentPhaseIndexLiveData.postValue(-1); // Этот LiveData будет обновлен в startNextPhase
            this.currentTaskIdForCycle = taskId;
            this.currentUserIdForCycle = userId;
            this.currentDbSessionIdForPhase = null;
            this.currentPhaseStartTime = null;
            this.accumulatedInterruptionsInCurrentPhase.set(0);

            startNextPhaseOrStopInternal(finalPhases, -1);
        });
    }

    private void startNextPhaseOrStopInternal(List<PomodoroPhase> phasesForCycle, int completedPhaseIndex) {
        synchronized (timerJobLock) {
            cancelActiveTimerJobInternal();
        }
        stopCurrentOperationAndSound();

        int newIndex = completedPhaseIndex + 1;
        PomodoroPhase phaseToStart = (newIndex < phasesForCycle.size()) ? phasesForCycle.get(newIndex) : null;

        Long taskId = currentTaskIdForCycle;
        Integer userId = currentUserIdForCycle;

        if (phaseToStart == null || taskId == null || userId == null) {
            logger.info(TAG, "All phases done or critical data (taskId/userId) missing. Cycle finished. Next Index: " + newIndex + ", Total Phases in cycle: " + phasesForCycle.size());
            setIdleStateAndResetCycleInternals(true);
            return;
        }
        // Обновляем LiveData, на которые подписан ViewModel, перед запуском самой фазы
        _pomodoroPhasesLiveData.postValue(new ArrayList<>(phasesForCycle)); // Убедимся, что ViewModel видит актуальные фазы
        _currentPhaseIndexLiveData.postValue(newIndex);
        logger.info(TAG, "Engine starting phase " + (newIndex + 1) + "/" + phasesForCycle.size() +
                ": " + phaseToStart.getType() + ", Duration: " + phaseToStart.getDurationSeconds() + "s");

        ListenableFuture<PomodoroSession> sessionFuture = pomodoroSessionManager.createNewPhaseSession(taskId, userId, phaseToStart);
        Futures.addCallback(sessionFuture, new FutureCallback<PomodoroSession>() {
            @Override
            public void onSuccess(PomodoroSession createdDbSession) {
                if (createdDbSession == null) {
                    onFailure(new IllegalStateException("Created DB session is null"));
                    return;
                }
                currentDbSessionIdForPhase = createdDbSession.getId();
                currentPhaseStartTime = dateTimeUtils.currentUtcDateTime();
                accumulatedInterruptionsInCurrentPhase.set(0);

                TimerState.Running newTimerState = new TimerState.Running(
                        phaseToStart.getDurationSeconds(),
                        phaseToStart.getDurationSeconds(),
                        phaseToStart.getType(),
                        0
                );
                _timerStateLiveData.postValue(newTimerState);
                launchTimerTask(newTimerState);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to create DB session for phase " + phaseToStart.getType() + ". Cycle stop.", t);
                setIdleStateAndResetCycleInternals(true);
            }
        }, engineExecutor);
    }


    private void launchTimerTask(TimerState.Running initialRunningState) {
        synchronized (timerJobLock) { // Синхронизируем доступ к activeTimerJob
            cancelActiveTimerJobInternal(); // Убедимся, что предыдущий точно отменен
            AtomicInteger remaining = new AtomicInteger(initialRunningState.getRemainingSeconds());
            logger.debug(TAG, "Launching timer task for " + initialRunningState.getType() + " with " + remaining.get() + " seconds.");

            activeTimerJob = engineExecutor.submit(() -> {
                try {
                    while (remaining.get() > 0) { // Убрали !Thread.currentThread().isInterrupted() из условия цикла
                        if (Thread.currentThread().isInterrupted()) { // Проверяем прерывание в начале итерации
                            logger.info(TAG, "Timer task for " + initialRunningState.getType() + " was interrupted (inside loop check). Remaining: " + remaining.get());
                            break; // Выходим из цикла, если поток прерван
                        }
                        Thread.sleep(1000);
                        // Повторная проверка на прерывание после sleep
                        if (Thread.currentThread().isInterrupted()) {
                            logger.info(TAG, "Timer task for " + initialRunningState.getType() + " was interrupted (after sleep). Remaining: " + remaining.get());
                            break;
                        }

                        int currentRemaining = remaining.decrementAndGet();
                        TimerState currentStateOnMainThread = _timerStateLiveData.getValue(); // Читаем на том же потоке, что и postValue

                        if (currentStateOnMainThread instanceof TimerState.Running &&
                                ((TimerState.Running) currentStateOnMainThread).getType() == initialRunningState.getType() &&
                                // Дополнительная проверка, чтобы убедиться, что это тот же таймер
                                Objects.equals(activeTimerJob, Thread.currentThread().isInterrupted() ? null : activeTimerJob)) {

                            TimerState.Running updatedState = ((TimerState.Running) currentStateOnMainThread).copy(
                                    currentRemaining, null, null, null
                            );
                            // Обновляем LiveData всегда на главном потоке, если у вас есть MainExecutor
                            // Если нет, то postValue() безопасен
                            _timerStateLiveData.postValue(updatedState);
                        } else {
                            logger.info(TAG, "Timer task for " + initialRunningState.getType() + " exiting. Current state: " +
                                    (currentStateOnMainThread != null ? currentStateOnMainThread.getClass().getSimpleName() : "null") +
                                    ", Expected type: " + initialRunningState.getType());
                            break;
                        }
                    }
                    // Проверяем, был ли цикл прерван или завершился естественно
                    if (remaining.get() == 0 && !Thread.currentThread().isInterrupted()) {
                        logger.info(TAG, "Timer task for " + initialRunningState.getType() + " completed naturally.");
                        // Вызов handleNaturalPhaseCompletionInternal должен быть на engineExecutor
                        engineExecutor.execute(this::handleNaturalPhaseCompletionInternal);
                    } else if (Thread.currentThread().isInterrupted()){
                        logger.info(TAG, "Timer task for " + initialRunningState.getType() + " did not complete naturally due to interruption. Remaining: " + remaining.get());
                    }
                } catch (InterruptedException e) {
                    logger.info(TAG, "Timer task for " + initialRunningState.getType() + " explicitly interrupted via InterruptedException. Remaining: " + remaining.get());
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error(TAG, "Exception in timer task for " + initialRunningState.getType(), e);
                    engineExecutor.execute(() -> setIdleStateAndResetCycleInternals(true));
                } finally {
                    synchronized (timerJobLock) {
                        // Обнуляем activeTimerJob только если это был текущий джоб
                        if (activeTimerJob != null && activeTimerJob.isDone()) { // Проверяем, что это тот же джоб и он завершен
                            activeTimerJob = null;
                        }
                    }
                    logger.debug(TAG, "Timer task for " + initialRunningState.getType() + " finished execution block.");
                }
            });
        }
    }

    public void pause() {
        engineExecutor.execute(() -> {
            logger.debug(TAG, "CMD: Pause");
            synchronized (timerJobLock) {
                cancelActiveTimerJobInternal();
            }
            TimerState currentState = _timerStateLiveData.getValue();
            if (currentState instanceof TimerState.Running) {
                int interruptions = accumulatedInterruptionsInCurrentPhase.incrementAndGet();
                _timerStateLiveData.postValue(new TimerState.Paused(
                        ((TimerState.Running) currentState).getRemainingSeconds(),
                        ((TimerState.Running) currentState).getTotalSeconds(),
                        ((TimerState.Running) currentState).getType(),
                        interruptions
                ));
            } else {
                logger.warn(TAG, "Cannot pause, current state is not Running: " + (currentState != null ? currentState.getClass().getSimpleName() : "null"));
            }
        });
    }

    public void resume() {
        engineExecutor.execute(() -> {
            logger.debug(TAG, "CMD: Resume");
            TimerState currentState = _timerStateLiveData.getValue();
            if (currentState instanceof TimerState.Paused) {
                TimerState.Running newRunningState = new TimerState.Running(
                        ((TimerState.Paused) currentState).getRemainingSeconds(),
                        ((TimerState.Paused) currentState).getTotalSeconds(),
                        ((TimerState.Paused) currentState).getType(),
                        ((TimerState.Paused) currentState).getInterruptions()
                );
                _timerStateLiveData.postValue(newRunningState);
                launchTimerTask(newRunningState);
            } else {
                logger.warn(TAG, "Cannot resume, current state is not Paused: " + (currentState != null ? currentState.getClass().getSimpleName() : "null"));
            }
        });
    }

    private void handleNaturalPhaseCompletionInternal() {
        Integer phaseIndex = _currentPhaseIndexLiveData.getValue();
        List<PomodoroPhase> phases = _pomodoroPhasesLiveData.getValue();
        if (phaseIndex == null || phases == null || phases.isEmpty() || phaseIndex < 0 || phaseIndex >= phases.size()) {
            logger.error(TAG, "Cannot handle natural phase completion: invalid phaseIndex or phases list. Index: " + phaseIndex + ", Phases size: " + (phases != null ? phases.size() : "null"));
            setIdleStateAndResetCycleInternals(true);
            return;
        }
        PomodoroPhase completedPhase = phases.get(phaseIndex);
        finalizeAndProcessPhaseInternal(false, completedPhase, phases, phaseIndex);
    }

    private void finalizeAndProcessPhaseInternal(boolean isStopCommand, PomodoroPhase phaseBeingFinalized, List<PomodoroPhase> cyclePhases, int currentPhaseIndexInCycle) {
        Long taskId = currentTaskIdForCycle;
        Long dbSessionId = currentDbSessionIdForPhase;
        LocalDateTime phaseStartTimeSnapshot = currentPhaseStartTime;
        int interruptions = accumulatedInterruptionsInCurrentPhase.get();

        if (phaseBeingFinalized == null || taskId == null || dbSessionId == null || phaseStartTimeSnapshot == null) {
            logger.error(TAG, "finalizeAndProcessPhase: Missing critical data for phase: " + phaseBeingFinalized);
            if (!isStopCommand && !(_timerStateLiveData.getValue() instanceof TimerState.Idle)) {
                setIdleStateAndResetCycleInternals(true);
            }
            return;
        }
        long actualDurationLong = Duration.between(phaseStartTimeSnapshot, dateTimeUtils.currentUtcDateTime()).getSeconds();
        int actualDurationSeconds = (int) Math.max(0, actualDurationLong);

        ListenableFuture<Void> completeFuture = completePomodoroSessionUseCase.execute(
                dbSessionId, taskId, phaseBeingFinalized.getType(),
                actualDurationSeconds, interruptions
        );

        Futures.addCallback(completeFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (!isStopCommand) {
                    _timerStateLiveData.postValue(new TimerState.WaitingForConfirmation(
                            phaseBeingFinalized.getType(),
                            phaseBeingFinalized.getDurationSeconds(),
                            accumulatedInterruptionsInCurrentPhase.get()
                    ));
                    playCompletionSoundAndVibration(phaseBeingFinalized.getType());
                }
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to process phase finalization for session " + dbSessionId, t);
                if (!isStopCommand) {
                    _timerStateLiveData.postValue(TimerState.Idle.getInstance());
                    playCompletionSoundAndVibration(phaseBeingFinalized.getType());
                }
                resetCycleStateInternals(isStopCommand); // Если stop, то частичный сброс
            }
        }, engineExecutor);
    }

    public void confirmCompletionAndProceed() {
        engineExecutor.execute(() -> {
            TimerState currentState = _timerStateLiveData.getValue();
            if (currentState instanceof TimerState.WaitingForConfirmation) {
                stopCurrentOperationAndSound();
                List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();
                Integer currentIndex = _currentPhaseIndexLiveData.getValue();
                if (currentPhases != null && currentIndex != null) {
                    startNextPhaseOrStopInternal(currentPhases, currentIndex);
                } else {
                    logger.error(TAG,"Cannot proceed after confirmation: phases or index is null.");
                    setIdleStateAndResetCycleInternals(true);
                }
            }
        });
    }

    public void skipCurrentBreak() {
        engineExecutor.execute(() -> {
            synchronized (timerJobLock) {
                cancelActiveTimerJobInternal();
            }
            Integer phaseIndex = _currentPhaseIndexLiveData.getValue();
            List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();

            if (phaseIndex == null || currentPhases == null || phaseIndex < 0 || phaseIndex >= currentPhases.size()) return;
            PomodoroPhase currentPhaseSkipping = currentPhases.get(phaseIndex);

            if (!currentPhaseSkipping.isBreak() || _timerStateLiveData.getValue() instanceof TimerState.Idle) {
                logger.warn(TAG, "CMD: Skip Break invalid. Phase: " + currentPhaseSkipping.getType() + ", State: " + _timerStateLiveData.getValue());
                return;
            }
            logger.info(TAG, "CMD: Skip Break. Phase: " + currentPhaseSkipping.getType() + ", Index: " + phaseIndex);
            stopCurrentOperationAndSound();
            finalizeAndProcessPhaseInternal(false, currentPhaseSkipping, currentPhases, phaseIndex); // Завершаем перерыв

            // После завершения перерыва, confirmCompletionAndProceed вызовет startNextPhaseOrStopInternal,
            // который автоматически перейдет к следующей фазе (которая должна быть фокусом, если есть)
            // или завершит цикл. Нам не нужно искать следующую фокус-фазу здесь.
        });
    }

    public void stopPomodoroCycle(boolean saveCurrentPhaseProgress) {
        engineExecutor.execute(() -> {
            TimerState currentStateSnapshot = _timerStateLiveData.getValue();
            logger.info(TAG, "CMD: Stop Cycle. Save: " + saveCurrentPhaseProgress +
                    ", State: " + (currentStateSnapshot != null ? currentStateSnapshot.getClass().getSimpleName() : "null") +
                    ", Index: " + _currentPhaseIndexLiveData.getValue());
            synchronized (timerJobLock) {
                cancelActiveTimerJobInternal();
            }
            stopCurrentOperationAndSound();

            List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();
            Integer currentIndex = _currentPhaseIndexLiveData.getValue();

            if (saveCurrentPhaseProgress &&
                    currentStateSnapshot != null &&
                    !(currentStateSnapshot instanceof TimerState.Idle) &&
                    currentPhases != null && !currentPhases.isEmpty() &&
                    currentIndex != null && currentIndex >= 0 && currentIndex < currentPhases.size() &&
                    currentTaskIdForCycle != null && currentDbSessionIdForPhase != null && currentPhaseStartTime != null) {
                if (!(currentStateSnapshot instanceof TimerState.WaitingForConfirmation)) {
                    finalizeAndProcessPhaseInternal(true, currentPhases.get(currentIndex), currentPhases, currentIndex);
                }
            }
            // Важно: сбрасываем состояние, но сохраняем taskId, userId и originalPhasesForCurrentCycle
            setIdleStateAndResetCycleInternals(false);
        });
    }


    private void setIdleStateAndResetCycleInternals(boolean fullReset) {
        if (!(_timerStateLiveData.getValue() instanceof TimerState.Idle)) {
            _timerStateLiveData.postValue(TimerState.Idle.getInstance());
        }
        resetCycleStateInternals(fullReset);
    }

    private void resetCycleStateInternals(boolean fullReset) {
        // LiveData для UI
        if (_pomodoroPhasesLiveData.getValue() != null && !_pomodoroPhasesLiveData.getValue().isEmpty()) {
            _pomodoroPhasesLiveData.postValue(fullReset ? Collections.emptyList() : new ArrayList<>(originalPhasesForCurrentCycle));
        }
        if (_currentPhaseIndexLiveData.getValue() != null && _currentPhaseIndexLiveData.getValue() != -1) {
            _currentPhaseIndexLiveData.postValue(-1);
        }

        // Внутреннее состояние текущей фазы
        currentDbSessionIdForPhase = null;
        currentPhaseStartTime = null;
        accumulatedInterruptionsInCurrentPhase.set(0);
        pomodoroSessionManager.resetCurrentPhaseSessionTracking();

        if (fullReset) {
            currentTaskIdForCycle = null;
            currentUserIdForCycle = null;
            originalPhasesForCurrentCycle = Collections.emptyList();
            logger.debug(TAG, "Pomodoro cycle internal state FULLY reset (including task/user/originalPhases).");
        } else {
            logger.debug(TAG, "Pomodoro cycle internal state reset (task/user/originalPhases preserved for potential restart).");
        }
    }

    private void stopCurrentOperationAndSound() {
        soundManager.stop();
        vibrationManager.stopVibrationLoop();
    }

    private void playCompletionSoundAndVibration(SessionType type) {
        PomodoroSettings settings = currentSettingsRef.get();
        if (settings == null) return; // Добавил проверку на null
        String soundUriString = type.isFocus() ? settings.getFocusSoundUri() : settings.getBreakSoundUri();
        if (soundUriString != null && !soundUriString.isEmpty()) {
            try {
                soundManager.playSoundLoop(Uri.parse(soundUriString));
            } catch (Exception e) {
                logger.error(TAG, "Failed to play sound: " + soundUriString, e);
            }
        }
        if (settings.isVibrationEnabled()) {
            vibrationManager.startVibrationLoop();
        }
    }

    public InterruptedPhaseInfo getCurrentInterruptedPhaseInfo() {
        PomodoroPhase phase = _currentPhaseLiveData.getValue();
        Long dbId = currentDbSessionIdForPhase;
        LocalDateTime startTime = currentPhaseStartTime;
        Long taskId = currentTaskIdForCycle;
        int interruptions = accumulatedInterruptionsInCurrentPhase.get();

        if (phase != null && dbId != null && startTime != null && taskId != null) {
            return new InterruptedPhaseInfo(dbId, phase.getType(), startTime, interruptions);
        }
        return null;
    }

    public void shutdown() {
        logger.debug(TAG, "Shutting down TimerEngine.");
        synchronized (timerJobLock) {
            cancelActiveTimerJobInternal();
        }
        stopCurrentOperationAndSound();
        engineExecutor.shutdownNow();
        settingsExecutor.shutdownNow();
        soundManager.shutdown();
        vibrationManager.shutdown();
        settingsRepository.getSettingsFlow().removeObserver(settingsObserver);
    }
    private final Observer<PomodoroSettings> settingsObserver = settings -> {
        if (settings != null) {
            PomodoroSettings oldSettings = currentSettingsRef.getAndSet(settings);
            logger.debug(TAG, "Settings updated via observer: " + settings);
            if (_timerStateLiveData.getValue() instanceof TimerState.Idle &&
                    (_pomodoroPhasesLiveData.getValue() == null || _pomodoroPhasesLiveData.getValue().isEmpty()) &&
                    oldSettings.getWorkDurationMinutes() != settings.getWorkDurationMinutes()) {
                _timerStateLiveData.postValue(TimerState.Idle.getInstance());
            }
        }
    };
    public void removeSettingsObserver() {
        settingsRepository.getSettingsFlow().removeObserver(settingsObserver);
    }

    private void cancelActiveTimerJobInternal() {
        Future<?> jobToCancel = activeTimerJob;
        if (jobToCancel != null) {
            if (!jobToCancel.isDone() && !jobToCancel.isCancelled()) {
                logger.debug(TAG, "Cancelling active timer job: " + jobToCancel.hashCode());
                jobToCancel.cancel(true);
            }
            activeTimerJob = null;
        }
    }
}