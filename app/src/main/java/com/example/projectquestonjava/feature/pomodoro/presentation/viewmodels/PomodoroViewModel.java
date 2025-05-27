package com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.R; // Для ресурсов, если понадобятся
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.SingleLiveEvent;
import com.example.projectquestonjava.feature.pomodoro.data.managers.PomodoroManager;
import com.example.projectquestonjava.feature.pomodoro.data.service.PomodoroTimerService;
import com.example.projectquestonjava.feature.pomodoro.domain.logic.PomodoroCycleGenerator;
import com.example.projectquestonjava.feature.pomodoro.domain.model.InterruptedPhaseInfo;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroPhase;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.pomodoro.domain.model.TimerState;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSessionRepository;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSettingsRepository;
import com.example.projectquestonjava.feature.pomodoro.domain.usecases.ForceCompleteTaskWithPomodoroUseCase;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

// PomodoroUiState data class (как был определен ранее)
// class PomodoroUiState { ... }

@HiltViewModel
public class PomodoroViewModel extends ViewModel {

    public static final int MAX_RECOMMENDED_TASK_DURATION_MINUTES = 4 * 60;
    private static final String TAG = "PomodoroViewModel";

    private final TaskRepository taskRepository;
    private final PomodoroManager pomodoroManager;
    private final PomodoroSettingsRepository settingsRepository;
    private final PomodoroCycleGenerator pomodoroCycleGenerator;
    private final ForceCompleteTaskWithPomodoroUseCase forceCompleteTaskWithPomodoroUseCase;
    private final SavedStateHandle savedStateHandle;
    private final Context applicationContext;
    private final Executor ioExecutor;
    private final Logger logger;
    private final SnackbarManager snackbarManager; // Добавлено

    // --- ИСПРАВЛЕННЫЕ И ДОБАВЛЕННЫЕ MutableLiveData ---
    private final MutableLiveData<PomodoroUiState> _uiStateLiveData;
    public final LiveData<PomodoroUiState> uiStateLiveData; // Публичный LiveData

    private final MutableLiveData<Task> _currentTaskLiveData;
    public final LiveData<Task> currentTaskLiveData;

    private final MutableLiveData<List<Task>> _upcomingTasksLiveData; // Был public LiveData, делаем private Mutable
    public final LiveData<List<Task>> upcomingTasksLiveData;

    private final MutableLiveData<Boolean> _showTaskSelectorLiveData;
    public final LiveData<Boolean> showTaskSelectorLiveData;

    private final MutableLiveData<List<PomodoroPhase>> _generatedPhasesLiveData; // Был public LiveData
    public final LiveData<List<PomodoroPhase>> generatedPhasesFlow; // Имя оставлено для совместимости с кодом

    private final MutableLiveData<Integer> _currentPhaseIndexLiveData; // Был public LiveData
    public final LiveData<Integer> currentPhaseIndexFlow; // Имя оставлено

    // Эти LiveData нужны для получения состояния от сервиса
    private final MutableLiveData<TimerState> _timerStateFromService;
    private final MutableLiveData<List<PomodoroPhase>> _phasesFromService;
    // --- КОНЕЦ ИСПРАВЛЕННЫХ И ДОБАВЛЕННЫХ MutableLiveData ---


    public final LiveData<Integer> sessionCountLiveData;

    private PomodoroTimerService.PomodoroBinder pomodoroBinder = null;
    private final MutableLiveData<Boolean> _serviceBoundLiveData;
    private final AtomicBoolean isBindingOperationInProgress = new AtomicBoolean(false);
    private final AtomicBoolean isUnbindingOperationInProgress = new AtomicBoolean(false);

    private Observer<TimerState> timerStateObserverFromService;
    private Observer<List<PomodoroPhase>> phasesObserverFromService;
    private Observer<Integer> phaseIndexObserverFromService;
    private final Observer<PomodoroSettings> settingsObserver;
    private final Observer<List<Task>> upcomingTasksRepoObserver;

    // Snackbar теперь через SnackbarManager, но оставим SingleLiveEvent для навигации (если нужно)
    private final SingleLiveEvent<String> _snackbarMessageEventInternal = new SingleLiveEvent<>(); // Переименован
    public final LiveData<String> snackbarMessageEvent = _snackbarMessageEventInternal; // Публичный доступ

    private final SingleLiveEvent<Boolean> _navigateToSettingsEvent = new SingleLiveEvent<>(); // Для навигации
    public final LiveData<Boolean> navigateToSettingsEvent = _navigateToSettingsEvent; // Публичный доступ


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pomodoroBinder = (PomodoroTimerService.PomodoroBinder) service;
            _serviceBoundLiveData.postValue(true);
            isBindingOperationInProgress.set(false);
            logger.debug(TAG, "Service connected. Binder: " + pomodoroBinder);
            subscribeToServiceState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logger.warn(TAG, "Service unexpectedly disconnected.");
            unsubscribeFromServiceState();
            pomodoroBinder = null;
            _serviceBoundLiveData.postValue(false);
            isBindingOperationInProgress.set(false);
            PomodoroUiState currentUi = _uiStateLiveData.getValue();
            resetToIdleSetupState(currentUi != null ? currentUi.pomodoroSettings : new PomodoroSettings());
        }
    };

    @Inject
    public PomodoroViewModel(
            TaskRepository taskRepository, PomodoroManager pomodoroManager,
            PomodoroSessionRepository pomodoroSessionRepository, PomodoroSettingsRepository settingsRepository,
            PomodoroCycleGenerator pomodoroCycleGenerator, ForceCompleteTaskWithPomodoroUseCase forceCompleteTaskWithPomodoroUseCase,
            SavedStateHandle savedStateHandle, @ApplicationContext Context context,
            @IODispatcher Executor ioExecutor, Logger logger, SnackbarManager snackbarManager) { // SnackbarManager добавлен
        this.taskRepository = taskRepository;
        this.pomodoroManager = pomodoroManager;
        this.settingsRepository = settingsRepository;
        this.pomodoroCycleGenerator = pomodoroCycleGenerator;
        this.forceCompleteTaskWithPomodoroUseCase = forceCompleteTaskWithPomodoroUseCase;
        this.savedStateHandle = savedStateHandle;
        this.applicationContext = context;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.snackbarManager = snackbarManager; // Сохраняем

        logger.info(TAG, "ViewModel initialized. Instance: " + this.hashCode());

        _uiStateLiveData = new MutableLiveData<>(new PomodoroUiState());
        uiStateLiveData = _uiStateLiveData;
        _currentTaskLiveData = new MutableLiveData<>(null);
        currentTaskLiveData = _currentTaskLiveData;
        _upcomingTasksLiveData = new MutableLiveData<>(Collections.emptyList());
        upcomingTasksLiveData = _upcomingTasksLiveData;
        _showTaskSelectorLiveData = new MutableLiveData<>(false);
        showTaskSelectorLiveData = _showTaskSelectorLiveData;
        _generatedPhasesLiveData = new MutableLiveData<>(Collections.emptyList());
        generatedPhasesFlow = _generatedPhasesLiveData;
        _currentPhaseIndexLiveData = new MutableLiveData<>(-1);
        currentPhaseIndexFlow = _currentPhaseIndexLiveData;
        _serviceBoundLiveData = new MutableLiveData<>(false);

        _timerStateFromService = new MutableLiveData<>(TimerState.Idle.getInstance());
        _phasesFromService = new MutableLiveData<>(Collections.emptyList());

        sessionCountLiveData = Transformations.switchMap(_currentTaskLiveData, task ->
                (task == null || task.getId() == 0) ? new MutableLiveData<>(0) : pomodoroSessionRepository.getCompletedSessionsCount(task.getId())
        );

        // Определяем Observer-ы
        timerStateObserverFromService = engineState -> {
            if (engineState != null) {
                logger.debug(TAG, "Observed TimerState from Service: " + engineState.getClass().getSimpleName());
                _timerStateFromService.postValue(engineState);
                combineAndPostUiState();
            }
        };
        phasesObserverFromService = phases -> {
            logger.debug(TAG, "Observed Phases from Service: " + (phases != null ? phases.size() : "null"));
            _phasesFromService.postValue(phases != null ? new ArrayList<>(phases) : Collections.emptyList());
            _generatedPhasesLiveData.postValue(phases != null ? new ArrayList<>(phases) : Collections.emptyList());
            combineAndPostUiState();
        };
        phaseIndexObserverFromService = index -> {
            logger.debug(TAG, "Observed PhaseIndex from Service: " + index);
            _currentPhaseIndexLiveData.postValue(index != null ? index : -1);
            combineAndPostUiState();
        };
        settingsObserver = newSettings -> {
            PomodoroUiState currentUi = _uiStateLiveData.getValue();
            if (currentUi != null && newSettings != null) {
                boolean wasSetupMode = currentUi.isTimeSetupMode;
                int newHours = currentUi.estimatedHours;
                int newMinutes = currentUi.estimatedMinutes;
                if (wasSetupMode && currentUi.estimatedHours * 60 + currentUi.estimatedMinutes == currentUi.pomodoroSettings.getWorkDurationMinutes()) {
                    newHours = newSettings.getWorkDurationMinutes() / 60;
                    newMinutes = newSettings.getWorkDurationMinutes() % 60;
                }
                final int finalNewHours = newHours;
                final int finalNewMinutes = newMinutes;
                // Вызываем updateUiState с флагами очистки null
                updateUiState(s -> s.copy(null, null, null, null, null, null, null, finalNewHours, finalNewMinutes, newSettings, null, null, null, false, false));
                if (wasSetupMode) {
                    updateGeneratedPhasesForCurrentEstimatedTime(finalNewHours, finalNewMinutes, _currentTaskLiveData.getValue());
                }
            }
        };
        upcomingTasksRepoObserver = tasks -> {
            _upcomingTasksLiveData.postValue(tasks != null ? tasks : Collections.emptyList());
            PomodoroUiState currentUi = _uiStateLiveData.getValue();
            Task currentTaskVal = _currentTaskLiveData.getValue();
            if (currentTaskVal == null && tasks != null && !tasks.isEmpty()) {
                Task newCurrent = tasks.get(0);
                _currentTaskLiveData.postValue(newCurrent);
                if (currentUi != null && currentUi.isTimeSetupMode) {
                    updateGeneratedPhasesForCurrentEstimatedTime(currentUi.estimatedHours, currentUi.estimatedMinutes, newCurrent);
                }
            }
        };

        settingsRepository.getSettingsFlow().observeForever(settingsObserver);
        taskRepository.getUpcomingTasks().observeForever(upcomingTasksRepoObserver);

        loadInitialTaskFromSavedState();
        bindToService();
    }

    // ... (loadInitialTaskFromSavedState, initializeDefaultUiState, bindToService, unbindFromService, subscribeToServiceState, unsubscribeFromServiceState)
    // Эти методы остаются такими же, как в предыдущей версии, но с учетом правильной инициализации LiveData полей.

    private void loadInitialTaskFromSavedState() {
        ioExecutor.execute(() -> {
            try {
                String taskIdString = savedStateHandle.get("taskId");
                Long taskId = (taskIdString != null) ? Long.parseLong(taskIdString) : null;
                PomodoroSettings initialSettings = Futures.getDone(settingsRepository.getSettings());

                if (taskId != null && taskId != -1L) {
                    Task initialTask = Futures.getDone(taskRepository.getTaskById(taskId));
                    if (initialTask != null) {
                        _currentTaskLiveData.postValue(initialTask);
                        int defaultMinutes = initialSettings.getWorkDurationMinutes();
                        updateUiState(s -> s.copy(null, null, null, null, null, null, null,
                                defaultMinutes / 60, defaultMinutes % 60, initialSettings, null, null, true, false, false));
                        updateGeneratedPhasesForCurrentEstimatedTime(defaultMinutes / 60, defaultMinutes % 60, initialTask);
                        logger.info(TAG, "Initial task loaded from SavedStateHandle: ID " + taskId);
                    } else {
                        logger.warn(TAG, "Task with ID " + taskId + " from SavedStateHandle not found. Using defaults.");
                        initializeDefaultUiState(initialSettings);
                    }
                } else {
                    logger.debug(TAG, "No taskId in SavedStateHandle. Initializing with default settings.");
                    initializeDefaultUiState(initialSettings);
                }
            } catch (Exception e) {
                logger.error(TAG, "Error loading initial task/settings", e);
                initializeDefaultUiState(new PomodoroSettings());
                updateUiState(s -> s.copy(null,null, null, "Ошибка загрузки задачи",null,null,null,null,null,null,null,null,null, false, true));
            }
        });
    }
    private void initializeDefaultUiState(PomodoroSettings settings) {
        int defaultMinutes = settings.getWorkDurationMinutes();
        _uiStateLiveData.postValue(new PomodoroUiState(
                TimerState.Idle.getInstance(),
                pomodoroManager.formatTime(defaultMinutes * 60),
                0f, null, SessionType.FOCUS, 0,0,
                defaultMinutes / 60,
                defaultMinutes % 60,
                settings, false, false, true
        ));
        updateGeneratedPhasesForCurrentEstimatedTime(defaultMinutes / 60, defaultMinutes % 60, _currentTaskLiveData.getValue());
    }

    private void bindToService() {
        if (_serviceBoundLiveData.getValue() == Boolean.TRUE || !isBindingOperationInProgress.compareAndSet(false, true)) {
            logger.debug(TAG, "Binding operation already in progress or service already bound.");
            return;
        }
        logger.debug(TAG, "Attempting to bind to PomodoroTimerService...");
        Intent serviceIntent = new Intent(applicationContext, PomodoroTimerService.class);
        try {
            boolean bound = applicationContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            if (!bound) {
                isBindingOperationInProgress.set(false);
                logger.error(TAG, "bindService returned false.");
                showErrorMessage("Не удалось подключиться к службе таймера.");
            } else {
                logger.info(TAG, "bindService call successful, waiting for onServiceConnected.");
            }
        } catch (Exception e) {
            isBindingOperationInProgress.set(false);
            logger.error(TAG, "Exception during bindService", e);
            showErrorMessage("Ошибка подключения к службе таймера: " + e.getMessage());
        }
    }

    private void unbindFromService() {
        if (_serviceBoundLiveData.getValue() == Boolean.TRUE && pomodoroBinder != null && !isUnbindingOperationInProgress.compareAndSet(false,true)) {
            logger.debug(TAG, "Unbinding from PomodoroTimerService...");
            try {
                applicationContext.unbindService(serviceConnection);
            } catch (Exception e) {
                logger.error(TAG, "Error unbinding from service", e);
            } finally {
                unsubscribeFromServiceState();
                pomodoroBinder = null;
                _serviceBoundLiveData.postValue(false);
                isUnbindingOperationInProgress.set(false);
                logger.info(TAG, "Service unbound handling finished.");
                PomodoroUiState currentUi = _uiStateLiveData.getValue();
                resetToIdleSetupState(currentUi != null ? currentUi.pomodoroSettings : new PomodoroSettings());
            }
        } else {
            logger.debug(TAG, "Service not bound or unbinding already in progress. isBound: " + _serviceBoundLiveData.getValue() + ", isUnbindingProgress: " + isUnbindingOperationInProgress.get());
            if (isUnbindingOperationInProgress.get()) isUnbindingOperationInProgress.set(false); // Сброс, если операция не началась
        }
    }
    private void subscribeToServiceState() {
        if (pomodoroBinder == null) {
            logger.warn(TAG, "Cannot subscribe to service state: binder is null.");
            return;
        }
        unsubscribeFromServiceState(); // Гарантируем отписку от предыдущих

        pomodoroBinder.getTimerStateLiveData().observeForever(timerStateObserverFromService);
        pomodoroBinder.getPomodoroPhasesLiveData().observeForever(phasesObserverFromService);
        pomodoroBinder.getCurrentPhaseIndexLiveData().observeForever(phaseIndexObserverFromService);
        logger.debug(TAG, "Subscribed to service state LiveData instances.");
        combineAndPostUiState();
    }

    private void unsubscribeFromServiceState() {
        if (pomodoroBinder != null) {
            if (timerStateObserverFromService != null) pomodoroBinder.getTimerStateLiveData().removeObserver(timerStateObserverFromService);
            if (phasesObserverFromService != null) pomodoroBinder.getPomodoroPhasesLiveData().removeObserver(phasesObserverFromService);
            if (phaseIndexObserverFromService != null) pomodoroBinder.getCurrentPhaseIndexLiveData().removeObserver(phaseIndexObserverFromService);
            logger.debug(TAG, "Unsubscribed from service state LiveData.");
        }
    }


    public synchronized void combineAndPostUiState() {
        TimerState engineState = _timerStateFromService.getValue();
        List<PomodoroPhase> phasesFromEngine = _phasesFromService.getValue();
        Integer indexFromEngine = _currentPhaseIndexLiveData.getValue();
        PomodoroSettings settings = settingsRepository.getSettingsFlow().getValue();
        Task task = _currentTaskLiveData.getValue();
        PomodoroUiState currentSnapshot = _uiStateLiveData.getValue();

        if (engineState == null) engineState = TimerState.Idle.getInstance();
        if (settings == null) settings = new PomodoroSettings();
        if (phasesFromEngine == null) phasesFromEngine = _generatedPhasesLiveData.getValue() != null ? _generatedPhasesLiveData.getValue() : Collections.emptyList();
        if (indexFromEngine == null) indexFromEngine = _currentPhaseIndexLiveData.getValue() != null ? _currentPhaseIndexLiveData.getValue() : -1;

        boolean newIsTimeSetupMode = currentSnapshot != null ? currentSnapshot.isTimeSetupMode : true;
        List<PomodoroPhase> phasesForUi;
        int phaseIndexForUi;
        int estHours = currentSnapshot != null ? currentSnapshot.estimatedHours : settings.getWorkDurationMinutes() / 60;
        int estMinutes = currentSnapshot != null ? currentSnapshot.estimatedMinutes : settings.getWorkDurationMinutes() % 60;

        if (engineState instanceof TimerState.Idle) {
            newIsTimeSetupMode = true;
            if (currentSnapshot != null && !(currentSnapshot.timerState instanceof TimerState.Idle)) {
                int defaultMinutes = settings.getWorkDurationMinutes();
                estHours = defaultMinutes / 60;
                estMinutes = defaultMinutes % 60;
            }
            int totalMinutes = estHours * 60 + estMinutes;
            phasesForUi = (task != null && totalMinutes >= PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES)
                    ? pomodoroCycleGenerator.generatePhases(totalMinutes)
                    : Collections.emptyList();
            phaseIndexForUi = -1;
            if (!Objects.equals(_generatedPhasesLiveData.getValue(), phasesForUi)) {
                _generatedPhasesLiveData.postValue(new ArrayList<>(phasesForUi));
            }
        } else {
            newIsTimeSetupMode = false;
            phasesForUi = phasesFromEngine;
            phaseIndexForUi = indexFromEngine;
            if (!Objects.equals(_generatedPhasesLiveData.getValue(), phasesForUi)) {
                _generatedPhasesLiveData.postValue(new ArrayList<>(phasesForUi));
            }
        }

        PomodoroUiState newState = buildNewUiState(
                engineState, settings, currentSnapshot != null ? currentSnapshot.errorMessage : null,
                estHours, estMinutes,
                phasesForUi, phaseIndexForUi,
                currentSnapshot != null ? currentSnapshot.showLongTaskWarningDialog : false,
                currentSnapshot != null ? currentSnapshot.isCompletingTaskEarly : false,
                newIsTimeSetupMode
        );
        _uiStateLiveData.postValue(newState);
        logger.debug(TAG, "UI State Combined: " + newState.timerState.getClass().getSimpleName() +
                ", time: " + newState.formattedTime + ", phases UI: " + phasesForUi.size() +
                ", index UI: " + phaseIndexForUi + ", setupMode: " + newState.isTimeSetupMode);
    }


    private PomodoroUiState buildNewUiState(
            TimerState timerState, PomodoroSettings settings, String error,
            int estimatedHours, int estimatedMinutes,
            List<PomodoroPhase> currentGeneratedPhases, Integer currentPhaseIndex,
            boolean showLongTaskWarningDialog, boolean isCompletingTaskEarly,
            boolean isTimeSetupMode
    ) {
        int remainingSeconds = 0;
        int totalSecondsInCurrentPhase = 0;
        SessionType currentPhaseTypeFromState = SessionType.FOCUS;

        if (timerState instanceof TimerState.Running) {
            remainingSeconds = ((TimerState.Running) timerState).getRemainingSeconds();
            totalSecondsInCurrentPhase = ((TimerState.Running) timerState).getTotalSeconds();
            currentPhaseTypeFromState = ((TimerState.Running) timerState).getType();
        } else if (timerState instanceof TimerState.Paused) {
            remainingSeconds = ((TimerState.Paused) timerState).getRemainingSeconds();
            totalSecondsInCurrentPhase = ((TimerState.Paused) timerState).getTotalSeconds();
            currentPhaseTypeFromState = ((TimerState.Paused) timerState).getType();
        } else if (timerState instanceof TimerState.WaitingForConfirmation) {
            totalSecondsInCurrentPhase = ((TimerState.WaitingForConfirmation) timerState).getTotalSeconds();
            currentPhaseTypeFromState = ((TimerState.WaitingForConfirmation) timerState).getType();
        } else if (timerState instanceof TimerState.Idle) {
            int idleDurationMinutes = isTimeSetupMode ? (estimatedHours * 60 + estimatedMinutes) : settings.getWorkDurationMinutes();
            remainingSeconds = Math.max(0, idleDurationMinutes * 60);
            totalSecondsInCurrentPhase = remainingSeconds;
        }

        float progress = 0f;
        if (totalSecondsInCurrentPhase > 0 && !(timerState instanceof TimerState.Idle)) {
            progress = 1f - ((float) remainingSeconds / totalSecondsInCurrentPhase);
        } else if (timerState instanceof TimerState.WaitingForConfirmation) {
            progress = 1f;
        }
        progress = Math.max(0f, Math.min(1f, progress));

        List<PomodoroPhase> phasesToUse = (currentGeneratedPhases != null) ? currentGeneratedPhases : Collections.emptyList();
        int actualPhaseIndex = (currentPhaseIndex != null) ? currentPhaseIndex : -1;

        int totalFocusSessions = (int) phasesToUse.stream().filter(PomodoroPhase::isFocus).count();
        int currentFocusSessionDisplayIndex = calculateFocusDisplayIndex(phasesToUse, actualPhaseIndex, timerState, totalFocusSessions);

        return new PomodoroUiState(
                timerState, pomodoroManager.formatTime(remainingSeconds), progress, error,
                currentPhaseTypeFromState, totalFocusSessions, currentFocusSessionDisplayIndex,
                estimatedHours, estimatedMinutes, settings, showLongTaskWarningDialog, isCompletingTaskEarly,
                isTimeSetupMode
        );
    }

    public void onEstimatedHoursChanged(String hoursText) {
        PomodoroUiState current = _uiStateLiveData.getValue();
        if (current == null || !current.isTimeSetupMode) return;
        int newHours = 0;
        try { newHours = Integer.parseInt(hoursText.replaceAll("[^0-9]", "")); } catch (NumberFormatException ignored) {}
        newHours = Math.max(0, Math.min(23, newHours));
        final int finalNewHours = newHours;
        updateUiState(s -> s.copy(null,null,null,null,null,null,null, finalNewHours, null, null,null,null, null, false, false));
        updateGeneratedPhasesForCurrentEstimatedTime(newHours, current.estimatedMinutes, _currentTaskLiveData.getValue());
    }

    public void onEstimatedMinutesChanged(String minutesText) {
        PomodoroUiState current = _uiStateLiveData.getValue();
        if (current == null || !current.isTimeSetupMode) return;
        int newMinutes = 0;
        try { newMinutes = Integer.parseInt(minutesText.replaceAll("[^0-9]", "")); } catch (NumberFormatException ignored) {}
        newMinutes = Math.max(0, Math.min(59, newMinutes));
        final int finalNewMinutes = newMinutes;
        updateUiState(s -> s.copy(null,null,null,null,null,null,null, null, finalNewMinutes, null,null,null, null, false, false));
        updateGeneratedPhasesForCurrentEstimatedTime(current.estimatedHours, newMinutes, _currentTaskLiveData.getValue());
    }


    public void updateGeneratedPhasesForCurrentEstimatedTime(int hours, int minutes, @Nullable Task task) {
        List<PomodoroPhase> newPhases;
        if (task != null) {
            int totalMinutes = hours * 60 + minutes;
            newPhases = (totalMinutes >= PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES)
                    ? pomodoroCycleGenerator.generatePhases(totalMinutes)
                    : Collections.emptyList();
        } else {
            newPhases = Collections.emptyList();
        }
        _generatedPhasesLiveData.postValue(newPhases);

        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        if (currentUi != null && currentUi.isTimeSetupMode) {
            _currentPhaseIndexLiveData.postValue(-1);
        }
        logger.debug(TAG, "Generated phases updated for " + hours + "h " + minutes + "m. Count: " + newPhases.size());
        combineAndPostUiState();
    }

    public void startOrToggleTimer() {
        PomodoroUiState currentVal = _uiStateLiveData.getValue();
        Task task = _currentTaskLiveData.getValue();
        if (currentVal == null) return;

        if (task == null && currentVal.timerState instanceof TimerState.Idle) {
            showErrorMessage("Сначала выберите задачу"); return;
        }

        TimerState currentEngineState = _timerStateFromService.getValue();
        if (currentEngineState == null) currentEngineState = TimerState.Idle.getInstance();


        if (currentEngineState instanceof TimerState.Running) pomodoroManager.pauseTimer();
        else if (currentEngineState instanceof TimerState.Paused) pomodoroManager.resumeTimer();
        else if (currentEngineState instanceof TimerState.Idle) {
            int totalMinutes = currentVal.estimatedHours * 60 + currentVal.estimatedMinutes;
            if (totalMinutes < PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
                showErrorMessage("Минимальное время: " + PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES + " мин."); return;
            }
            if (currentVal.showLongTaskWarningDialog) return;
            if (totalMinutes > MAX_RECOMMENDED_TASK_DURATION_MINUTES) {
                updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, true, null, null, false, false));
                return;
            }
            proceedWithPomodoroCycleStartInternal(task, totalMinutes);
        } else if (currentEngineState instanceof TimerState.WaitingForConfirmation) {
            pomodoroManager.confirmTimerCompletion();
        }
    }

    public void proceedWithPomodoroCycleStart() {
        proceedWithPomodoroCycleStartInternal(_currentTaskLiveData.getValue(), null);
    }

    private void proceedWithPomodoroCycleStartInternal(@Nullable Task taskForCycle, @Nullable Integer estimatedMinutesForCycle) {
        Task taskToUse = (taskForCycle != null) ? taskForCycle : _currentTaskLiveData.getValue();
        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        if (currentUi == null || taskToUse == null) {
            showErrorMessage(taskToUse == null ? "Задача не выбрана." : "Ошибка состояния UI.");
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, null, false, false));
            return;
        }

        int totalMinutesToUse = (estimatedMinutesForCycle != null) ?
                estimatedMinutesForCycle :
                (currentUi.estimatedHours * 60 + currentUi.estimatedMinutes);

        if (totalMinutesToUse < PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
            showErrorMessage("Время слишком мало.");
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, null, false, false));
            return;
        }

        List<PomodoroPhase> phases = pomodoroCycleGenerator.generatePhases(totalMinutesToUse);
        if (phases.isEmpty()) {
            showErrorMessage("Не удалось разбить задачу.");
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, null, false, false));
            return;
        }

        _generatedPhasesLiveData.postValue(new ArrayList<>(phases));
        _currentPhaseIndexLiveData.postValue(0);
        updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, false, false, false));
        pomodoroManager.startPomodoroCycle(taskToUse.getId(), taskToUse.getUserId(), new ArrayList<>(phases));
    }

    public void setCurrentTask(Task task) {
        Task previousTask = _currentTaskLiveData.getValue();
        _currentTaskLiveData.postValue(task);
        _showTaskSelectorLiveData.postValue(false);

        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        TimerState currentEngineState = _timerStateFromService.getValue();
        if (currentEngineState == null) currentEngineState = TimerState.Idle.getInstance();


        if (currentUi != null) {
            if (!(currentEngineState instanceof TimerState.Idle) && (previousTask == null || previousTask.getId() != task.getId())) {
                pomodoroManager.stopTimer();
            } else {
                resetToIdleSetupState(currentUi.pomodoroSettings);
                updateGeneratedPhasesForCurrentEstimatedTime(currentUi.estimatedHours, currentUi.estimatedMinutes, task);
            }
        }
    }

    private void resetToIdleSetupState(PomodoroSettings settingsToUse) {
        PomodoroSettings currentSettings = (settingsToUse != null) ? settingsToUse : new PomodoroSettings();
        int defaultMinutes = currentSettings.getWorkDurationMinutes();
        int defaultHours = defaultMinutes / 60;
        int finalDefaultMinutes = defaultMinutes % 60;

        updateUiState(s -> s.copy(
                TimerState.Idle.getInstance(), pomodoroManager.formatTime(defaultMinutes * 60), 0f, null,
                SessionType.FOCUS, 0, 0, defaultHours, finalDefaultMinutes, currentSettings,
                false, false, true, false, false)); // isTimeSetupMode = true

        _generatedPhasesLiveData.postValue(Collections.emptyList());
        _currentPhaseIndexLiveData.postValue(-1);
        logger.debug(TAG, "ViewModel reset to Idle/Setup mode.");
    }

    public void dismissLongTaskWarningDialog() { updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false,null, null, false, false)); }

    public void skipCurrentBreakAndStartNextFocus() {
        TimerState currentEngineState = _timerStateFromService.getValue();
        if (currentEngineState != null &&
                (currentEngineState instanceof TimerState.Running && ((TimerState.Running) currentEngineState).getType().isBreak() ||
                        currentEngineState instanceof TimerState.Paused && ((TimerState.Paused) currentEngineState).getType().isBreak() ||
                        currentEngineState instanceof TimerState.WaitingForConfirmation && ((TimerState.WaitingForConfirmation) currentEngineState).getType().isBreak())) {
            logger.info(TAG, "User skipping current break.");
            pomodoroManager.skipBreak();
        } else {
            logger.warn(TAG, "Skip break invalid. Engine state: " + (currentEngineState != null ? currentEngineState.getClass().getSimpleName() : "null"));
        }
    }

    public void completeTaskEarly() {
        Task task = _currentTaskLiveData.getValue();
        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        if (task == null || currentUi == null || currentUi.isCompletingTaskEarly) return;

        updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, null, true, null, false, true)); // clearError
        // showErrorMessage(null); // Уже делает clearError в updateUiState

        ioExecutor.execute(() -> {
            InterruptedPhaseInfo interruptedInfo = null;
            if (pomodoroBinder != null && _serviceBoundLiveData.getValue() == Boolean.TRUE) {
                try { interruptedInfo = pomodoroBinder.getCurrentInterruptedPhaseInfoFromEngine(); }
                catch (Exception e) { logger.error(TAG, "Error getting interrupted phase info from binder", e); }
            }

            final Task finalTask = task;
            final InterruptedPhaseInfo finalInterruptedInfo = interruptedInfo;

            TimerState currentEngineState = _timerStateFromService.getValue();
            if (currentEngineState != null && !(currentEngineState instanceof TimerState.Idle)) {
                pomodoroManager.stopTimer();
                try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }

            ListenableFuture<Void> completeFuture = forceCompleteTaskWithPomodoroUseCase.execute(finalTask.getId(), finalInterruptedInfo);
            Futures.addCallback(completeFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    _currentTaskLiveData.postValue(null);
                    showSnackbarMessage("Задача '" + finalTask.getTitle() + "' завершена.");
                    updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, null, false, null, false, false));
                }
                @Override
                public void onFailure(@NonNull Throwable t) {
                    showErrorMessage("Ошибка завершения: " + t.getMessage());
                    updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, null, false, null, false, false));
                }
            }, MoreExecutors.directExecutor());
        });
    }

    public void stopTimer() { pomodoroManager.stopTimer(); }
    public void toggleTaskSelector() { _showTaskSelectorLiveData.setValue(!Boolean.TRUE.equals(_showTaskSelectorLiveData.getValue())); }
    public void clearErrorMessage() { updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, null, null,null, true, false)); }


    public void navigateToSettings() { // Новый метод
        _navigateToSettingsEvent.setValue(true);
    }
    public void clearNavigateToSettings() { // Новый метод
        _navigateToSettingsEvent.setValue(null);
    }


    private void showErrorMessage(String message) {
        updateUiState(s -> s.copy(null,null,null,message,null,null,null, null,null,null, null, null,null, false, true));
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            PomodoroUiState current = _uiStateLiveData.getValue();
            if (current != null && Objects.equals(current.errorMessage, message)) {
                clearErrorMessage();
            }
        }, 3500);
    }

    private void showSnackbarMessage(String message) {
        snackbarManager.showMessage(message);
    }

    private int calculateFocusDisplayIndex(List<PomodoroPhase> phases, int currentIndex, TimerState timerState, int totalFocus) {
        if (phases == null || phases.isEmpty()) return (totalFocus > 0 && timerState instanceof TimerState.Idle) ? 1 : 0;
        if (currentIndex < 0 || currentIndex >= phases.size()) {
            PomodoroPhase firstFocus = phases.stream().filter(PomodoroPhase::isFocus).findFirst().orElse(null);
            return (totalFocus > 0 && timerState instanceof TimerState.Idle && firstFocus != null) ?
                    firstFocus.getTotalFocusSessionIndex() : 0;
        }
        PomodoroPhase currentPhase = phases.get(currentIndex);
        if (currentPhase.isFocus()) return currentPhase.getTotalFocusSessionIndex();
        if (currentIndex + 1 < phases.size() && phases.get(currentIndex + 1).isFocus()) {
            return phases.get(currentIndex + 1).getTotalFocusSessionIndex();
        }
        if (currentPhase.isBreak() && totalFocus > 0 &&
                (currentIndex == phases.size() - 1 || (currentIndex + 1 < phases.size() && !phases.get(currentIndex + 1).isFocus()))) {
            return totalFocus;
        }
        return Math.max(0, currentPhase.getTotalFocusSessionIndex());
    }

    private void updateUiState(UiStateUpdaterPomodoro updater) {
        PomodoroUiState current = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.update(current != null ? current : new PomodoroUiState()));
    }

    @FunctionalInterface
    interface UiStateUpdaterPomodoro { PomodoroUiState update(PomodoroUiState currentState); }

    @Override
    protected void onCleared() {
        super.onCleared();
        unsubscribeFromServiceState();
        if (_serviceBoundLiveData.getValue() == Boolean.TRUE) {
            try { applicationContext.unbindService(serviceConnection); }
            catch (Exception e) { logger.error(TAG, "Error unbinding service in onCleared", e); }
            _serviceBoundLiveData.setValue(false);
            pomodoroBinder = null;
        }
        if (settingsObserver != null) settingsRepository.getSettingsFlow().removeObserver(settingsObserver);
        if (upcomingTasksRepoObserver != null && upcomingTasksLiveData != null) { // Добавлена проверка на null для upcomingTasksLiveData
            upcomingTasksLiveData.removeObserver(upcomingTasksRepoObserver);
        }
        logger.debug(TAG, "ViewModel cleared and resources released.");
    }
}