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
    private final ExecutorService engineExecutor; // Для основной логики таймера
    private final ExecutorService settingsExecutor; // Отдельный для загрузки настроек
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

    private volatile Future<?> activeTimerJob = null; // Future для управления задачей таймера
    private final AtomicReference<PomodoroSettings> currentSettingsRef = new AtomicReference<>(new PomodoroSettings());

    @Inject
    public TimerEngine(
            SoundManager soundManager,
            VibrationManager vibrationManager,
            CompletePomodoroSessionUseCase completePomodoroSessionUseCase,
            PomodoroSettingsRepository settingsRepository,
            PomodoroSessionManager pomodoroSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor, // Принимаем ExecutorService
            Logger logger) {
        this.soundManager = soundManager;
        this.vibrationManager = vibrationManager;
        this.completePomodoroSessionUseCase = completePomodoroSessionUseCase;
        this.settingsRepository = settingsRepository;
        this.pomodoroSessionManager = pomodoroSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.engineExecutor = (ExecutorService) ioExecutor; // Используем внедренный Executor
        this.settingsExecutor = Executors.newSingleThreadExecutor(); // Для настроек, чтобы не блокировать engineExecutor
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
                // Используем addCallback вместо прямого get() чтобы обработать возможные ошибки Future
                Futures.addCallback(settingsFuture, new FutureCallback<PomodoroSettings>() {
                    @Override
                    public void onSuccess(@Nullable PomodoroSettings settings) {
                        if (settings == null) { // Дополнительная проверка, если Future вернул null
                            logger.error(TAG, "Initial settings loaded as null. Using defaults.");
                            currentSettingsRef.set(new PomodoroSettings());
                        } else {
                            currentSettingsRef.set(settings);
                            logger.debug(TAG, "Initial settings loaded: " + settings);
                        }
                        // Обновляем состояние Idle только если оно текущее и нет активных фаз
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
                        // Обновляем состояние Idle, если это уместно
                        TimerState currentTimerState = _timerStateLiveData.getValue();
                        List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();
                        if (currentTimerState instanceof TimerState.Idle &&
                                (currentPhases == null || currentPhases.isEmpty())) {
                            _timerStateLiveData.postValue(TimerState.Idle.getInstance());
                        }
                    }
                }, MoreExecutors.directExecutor()); // Коллбэк на том же потоке (settingsExecutor)

            } catch (Exception e) { // Ловим другие возможные RuntimeException на случай, если settingsRepository.getSettings() их кидает
                logger.error(TAG, "Unexpected error during initial settings load trigger", e);
                currentSettingsRef.set(new PomodoroSettings());
            }
        });
        settingsRepository.getSettingsFlow().observeForever(settingsObserver);
    }

    public void startPomodoroCycle(long taskId, int userId, List<PomodoroPhase> phases) {
        engineExecutor.execute(() -> { // Все команды выполняем на Executor
            logger.info(TAG, "CMD: Start Cycle. Task: " + taskId + ", User: " + userId + ", Phases: " + phases.size());
            cancelActiveTimerJob(); // Отменяем предыдущий таймер
            stopCurrentOperationAndSound(); // Останавливаем звук/вибрацию

            if (phases.isEmpty()) {
                logger.warn(TAG, "Cannot start cycle: phase list is empty.");
                setIdleStateAndResetCycleInternals();
                return;
            }

            _pomodoroPhasesLiveData.postValue(new ArrayList<>(phases)); // Копируем список
            _currentPhaseIndexLiveData.postValue(-1);
            this.currentTaskIdForCycle = taskId;
            this.currentUserIdForCycle = userId;
            this.currentDbSessionIdForPhase = null;
            this.currentPhaseStartTime = null;
            this.accumulatedInterruptionsInCurrentPhase.set(0);

            startNextPhaseOrStopInternal();
        });
    }

    private void startNextPhaseOrStopInternal() { // Должен вызываться из engineExecutor
        cancelActiveTimerJob();
        stopCurrentOperationAndSound();

        Integer currentIndex = _currentPhaseIndexLiveData.getValue();
        List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();
        if (currentIndex == null || currentPhases == null) {
            setIdleStateAndResetCycleInternals(); return;
        }

        int newIndex = currentIndex + 1;
        PomodoroPhase phaseToStart = (newIndex < currentPhases.size()) ? currentPhases.get(newIndex) : null;

        Long taskId = currentTaskIdForCycle; // Читаем volatile переменные
        Integer userId = currentUserIdForCycle;

        if (phaseToStart == null || taskId == null || userId == null) {
            logger.info(TAG, "All phases done or critical data missing. Cycle finished. Index: " + newIndex + ", Phases: " + currentPhases.size());
            setIdleStateAndResetCycleInternals();
            return;
        }

        _currentPhaseIndexLiveData.postValue(newIndex); // Обновляем индекс
        logger.info(TAG, "Engine starting phase " + (newIndex + 1) + "/" + currentPhases.size() +
                ": " + phaseToStart.getType() + ", Duration: " + phaseToStart.getDurationSeconds() + "s");

        ListenableFuture<PomodoroSession> sessionFuture = pomodoroSessionManager.createNewPhaseSession(taskId, userId, phaseToStart);
        Futures.addCallback(sessionFuture, new FutureCallback<PomodoroSession>() {
            @Override
            public void onSuccess(PomodoroSession createdDbSession) {
                if (createdDbSession == null) { // Дополнительная проверка
                    onFailure(new IllegalStateException("Created DB session is null"));
                    return;
                }
                currentDbSessionIdForPhase = createdDbSession.getId();
                currentPhaseStartTime = dateTimeUtils.currentUtcDateTime(); // Фиксируем время начала
                accumulatedInterruptionsInCurrentPhase.set(0);

                TimerState.Running newTimerState = new TimerState.Running(
                        phaseToStart.getDurationSeconds(),
                        phaseToStart.getDurationSeconds(),
                        phaseToStart.getType(),
                        0
                );
                _timerStateLiveData.postValue(newTimerState); // Обновляем LiveData
                launchTimerTask(newTimerState);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to create DB session for phase " + phaseToStart.getType() + ". Cycle stop.", t);
                setIdleStateAndResetCycleInternals();
            }
        }, engineExecutor); // Коллбэк на том же Executor
    }

    private void launchTimerTask(TimerState.Running initialRunningState) {
        cancelActiveTimerJob(); // Отменяем предыдущий, если есть
        activeTimerJob = engineExecutor.submit(() -> {
            AtomicInteger remaining = new AtomicInteger(initialRunningState.getRemainingSeconds());
            try {
                while (remaining.get() > 0 && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                    if (Thread.currentThread().isInterrupted()) break;

                    int currentRemaining = remaining.decrementAndGet();
                    TimerState currentState = _timerStateLiveData.getValue(); // Читаем текущее состояние

                    // Обновляем LiveData, только если мы все еще в состоянии Running
                    if (currentState instanceof TimerState.Running && ((TimerState.Running) currentState).getType() == initialRunningState.getType()) {
                        TimerState.Running updatedState = ((TimerState.Running) currentState).copy(
                                currentRemaining, null, null, null
                        );
                        _timerStateLiveData.postValue(updatedState);
                    } else {
                        // Состояние изменилось (например, на Paused или Stop), выходим из цикла
                        break;
                    }
                }
                // Проверяем, был ли цикл прерван или завершился естественно
                if (remaining.get() == 0 && !Thread.currentThread().isInterrupted()) {
                    handleNaturalPhaseCompletionInternal();
                }
            } catch (InterruptedException e) {
                logger.info(TAG, "Timer task for " + initialRunningState.getType() + " interrupted.");
                Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
            } catch (Exception e) {
                logger.error(TAG, "Exception in timer task for " + initialRunningState.getType(), e);
                setIdleStateAndResetCycleInternals(); // При ошибке сбрасываем состояние
            }
        });
    }

    public void pause() {
        engineExecutor.execute(() -> {
            cancelActiveTimerJob();
            TimerState currentState = _timerStateLiveData.getValue();
            if (currentState instanceof TimerState.Running) {
                int interruptions = accumulatedInterruptionsInCurrentPhase.incrementAndGet();
                _timerStateLiveData.postValue(new TimerState.Paused(
                        ((TimerState.Running) currentState).getRemainingSeconds(),
                        ((TimerState.Running) currentState).getTotalSeconds(),
                        ((TimerState.Running) currentState).getType(),
                        interruptions
                ));
            }
        });
    }

    public void resume() {
        engineExecutor.execute(() -> {
            TimerState currentState = _timerStateLiveData.getValue();
            if (currentState instanceof TimerState.Paused) {
                TimerState.Running newRunningState = new TimerState.Running(
                        ((TimerState.Paused) currentState).getRemainingSeconds(),
                        ((TimerState.Paused) currentState).getTotalSeconds(),
                        ((TimerState.Paused) currentState).getType(),
                        ((TimerState.Paused) currentState).getInterruptions() // Сохраняем счетчик прерываний
                );
                _timerStateLiveData.postValue(newRunningState);
                launchTimerTask(newRunningState);
            }
        });
    }

    private void handleNaturalPhaseCompletionInternal() { // Должен вызываться из engineExecutor
        Integer phaseIndex = _currentPhaseIndexLiveData.getValue();
        List<PomodoroPhase> phases = _pomodoroPhasesLiveData.getValue();
        if (phaseIndex == null || phases == null || phaseIndex < 0 || phaseIndex >= phases.size()) return;

        PomodoroPhase completedPhase = phases.get(phaseIndex);
        finalizeAndProcessPhaseInternal(false, completedPhase);
    }

    private void finalizeAndProcessPhaseInternal(boolean isStopCommand, PomodoroPhase phaseBeingFinalized) {
        // Этот метод теперь вызывается из engineExecutor (например, из handleNaturalPhaseCompletionInternal или stopPomodoroCycle)

        Long taskId = currentTaskIdForCycle;
        Long dbSessionId = currentDbSessionIdForPhase;
        LocalDateTime phaseStartTimeSnapshot = currentPhaseStartTime;
        int interruptions = accumulatedInterruptionsInCurrentPhase.get();

        if (phaseBeingFinalized == null || taskId == null || dbSessionId == null || phaseStartTimeSnapshot == null) {
            logger.error(TAG, "finalizeAndProcessPhase: Missing critical data for phase: " + phaseBeingFinalized);
            if (!isStopCommand && !(_timerStateLiveData.getValue() instanceof TimerState.Idle)) {
                setIdleStateAndResetCycleInternals();
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
                    List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();
                    Integer currentIdx = _currentPhaseIndexLiveData.getValue();
                    int nextPhaseTotalSeconds = (currentPhases != null && currentIdx != null && currentIdx + 1 < currentPhases.size()) ?
                            currentPhases.get(currentIdx + 1).getDurationSeconds() :
                            Objects.requireNonNull(currentSettingsRef.get()).getWorkDurationMinutes() * 60;

                    _timerStateLiveData.postValue(new TimerState.WaitingForConfirmation(
                            phaseBeingFinalized.getType(),
                            nextPhaseTotalSeconds, // Это totalSeconds следующей фазы, или текущей если последняя? По идее завершенной.
                            accumulatedInterruptionsInCurrentPhase.get()
                    ));
                    playCompletionSoundAndVibration(phaseBeingFinalized.getType());
                }
                // Если isStopCommand, состояние Idle и reset будут установлены в stopPomodoroCycle
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to process phase finalization for session " + dbSessionId, t);
                if (!isStopCommand) { // Если это не команда стоп, но произошла ошибка, все равно пытаемся проиграть звук
                    _timerStateLiveData.postValue(TimerState.Idle.getInstance()); // Переводим в Idle, т.к. ошибка
                    playCompletionSoundAndVibration(phaseBeingFinalized.getType()); // Звук об окончании (возможно, с ошибкой)
                }
                resetCycleStateInternals(); // Сбрасываем состояние цикла
            }
        }, engineExecutor); // Коллбэк выполняется на engineExecutor
    }


    public void confirmCompletionAndProceed() {
        engineExecutor.execute(() -> {
            if (_timerStateLiveData.getValue() instanceof TimerState.WaitingForConfirmation) {
                stopCurrentOperationAndSound();
                startNextPhaseOrStopInternal();
            }
        });
    }

    public void skipCurrentBreak() {
        engineExecutor.execute(() -> {
            cancelActiveTimerJob();
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
            finalizeAndProcessPhaseInternal(false, currentPhaseSkipping); // Завершаем текущий перерыв

            // Ищем следующую фокус-фазу
            int nextFocusIdx = -1;
            for (int i = phaseIndex + 1; i < currentPhases.size(); i++) {
                if (currentPhases.get(i).isFocus()) {
                    nextFocusIdx = i;
                    break;
                }
            }
            if (nextFocusIdx != -1) {
                _currentPhaseIndexLiveData.postValue(nextFocusIdx - 1); // Устанавливаем индекс ПЕРЕД следующей фокус-фазой
                startNextPhaseOrStopInternal();
            } else {
                logger.info(TAG, "No more focus phases after skipping. Cycle finished.");
                setIdleStateAndResetCycleInternals();
            }
        });
    }

    public void stopPomodoroCycle(boolean saveCurrentPhaseProgress) {
        engineExecutor.execute(() -> {
            TimerState currentStateSnapshot = _timerStateLiveData.getValue();
            logger.info(TAG, "CMD: Stop Cycle. Save: " + saveCurrentPhaseProgress +
                    ", State: " + (currentStateSnapshot != null ? currentStateSnapshot.getClass().getSimpleName() : "null") +
                    ", Index: " + _currentPhaseIndexLiveData.getValue());
            cancelActiveTimerJob();
            stopCurrentOperationAndSound();

            List<PomodoroPhase> currentPhases = _pomodoroPhasesLiveData.getValue();
            Integer currentIndex = _currentPhaseIndexLiveData.getValue();

            if (saveCurrentPhaseProgress &&
                    currentStateSnapshot != null &&
                    !(currentStateSnapshot instanceof TimerState.Idle) &&
                    !(currentStateSnapshot instanceof TimerState.WaitingForConfirmation) &&
                    currentPhases != null && currentIndex != null && currentIndex >= 0 && currentIndex < currentPhases.size() &&
                    currentTaskIdForCycle != null && currentDbSessionIdForPhase != null && currentPhaseStartTime != null) {
                finalizeAndProcessPhaseInternal(true, currentPhases.get(currentIndex));
            }
            setIdleStateAndResetCycleInternals();
        });
    }

    private void setIdleStateAndResetCycleInternals() { // Должен вызываться из engineExecutor
        if (!(_timerStateLiveData.getValue() instanceof TimerState.Idle)) {
            _timerStateLiveData.postValue(TimerState.Idle.getInstance());
        }
        resetCycleStateInternals();
    }

    private void resetCycleStateInternals() { // Должен вызываться из engineExecutor
        _pomodoroPhasesLiveData.postValue(Collections.emptyList());
        _currentPhaseIndexLiveData.postValue(-1);
        currentTaskIdForCycle = null;
        currentUserIdForCycle = null;
        currentDbSessionIdForPhase = null;
        currentPhaseStartTime = null;
        accumulatedInterruptionsInCurrentPhase.set(0);
        pomodoroSessionManager.resetCurrentPhaseSessionTracking();
        logger.debug(TAG, "Pomodoro cycle internal state has been reset.");
    }

    private void stopCurrentOperationAndSound() { // Должен вызываться из engineExecutor
        // Отменяем задачи звука/вибрации, если они запущены на engineExecutor
        // Если они на своих Executor'ах, то SoundManager/VibrationManager сами их остановят
        soundManager.stop();
        vibrationManager.stopVibrationLoop();
    }

    private void playCompletionSoundAndVibration(SessionType type) { // Должен вызываться из engineExecutor
        PomodoroSettings settings = currentSettingsRef.get();
        String soundUriString = type.isFocus() ? settings.getFocusSoundUri() : settings.getBreakSoundUri();
        if (soundUriString != null && !soundUriString.isEmpty()) {
            try {
                soundManager.playSoundLoop(Uri.parse(soundUriString)); // SoundManager сам управляет потоком
            } catch (Exception e) {
                logger.error(TAG, "Failed to play sound: " + soundUriString, e);
            }
        }
        if (settings.isVibrationEnabled()) {
            vibrationManager.startVibrationLoop(); // VibrationManager сам управляет потоком
        }
    }

    public InterruptedPhaseInfo getCurrentInterruptedPhaseInfo() {
        // Этот метод может быть вызван извне, поэтому доступ к volatile переменным должен быть безопасным
        PomodoroPhase phase = _currentPhaseLiveData.getValue(); // Используем LiveData
        Long dbId = currentDbSessionIdForPhase;
        LocalDateTime startTime = currentPhaseStartTime;
        Long taskId = currentTaskIdForCycle; // Для полноты информации
        int interruptions = accumulatedInterruptionsInCurrentPhase.get();

        if (phase != null && dbId != null && startTime != null && taskId != null) {
            return new InterruptedPhaseInfo(dbId, phase.getType(), startTime, interruptions);
        }
        return null;
    }

    public void shutdown() { // Вызывается при уничтожении сервиса или приложения
        logger.debug(TAG, "Shutting down TimerEngine.");
        cancelActiveTimerJob();
        stopCurrentOperationAndSound();
        engineExecutor.shutdownNow(); // Останавливаем все задачи в executor
        settingsExecutor.shutdownNow();
        soundManager.shutdown(); // Если SoundManager имеет свой Executor
        vibrationManager.shutdown(); // Если VibrationManager имеет свой Executor
        // Отписка от LiveData настроек
        settingsRepository.getSettingsFlow().removeObserver(settingsObserver);
    }
    // Экземпляр Observer для отписки
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
    // Для отписки в onDestroy сервиса, если TimerEngine не Singleton
    public void removeSettingsObserver() {
        settingsRepository.getSettingsFlow().removeObserver(settingsObserver);
    }


    private void cancelActiveTimerJob() {
        Future<?> job = activeTimerJob; // Читаем volatile один раз
        if (job != null) {
            job.cancel(true); // true для прерывания потока, если он в Thread.sleep()
            activeTimerJob = null;
        }
    }
}