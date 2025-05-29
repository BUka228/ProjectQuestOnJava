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
import com.example.projectquestonjava.R;
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
    private final SnackbarManager snackbarManager;

    private final MutableLiveData<PomodoroUiState> _uiStateLiveData;
    public final LiveData<PomodoroUiState> uiStateLiveData;

    private final MutableLiveData<Task> _currentTaskLiveData;
    public final LiveData<Task> currentTaskLiveData;

    private final MutableLiveData<List<Task>> _upcomingTasksLiveData;
    public final LiveData<List<Task>> upcomingTasksLiveData;

    private final MutableLiveData<Boolean> _showTaskSelectorLiveData;
    public final LiveData<Boolean> showTaskSelectorLiveData;

    private final MutableLiveData<List<PomodoroPhase>> _generatedPhasesLiveData;
    public final LiveData<List<PomodoroPhase>> generatedPhasesFlow;

    private final MutableLiveData<Integer> _currentPhaseIndexUiLiveData; // Для UI, может быть -1
    public final LiveData<Integer> currentPhaseIndexFlow;

    private final MutableLiveData<TimerState> _timerStateFromService;
    private final MutableLiveData<List<PomodoroPhase>> _phasesFromService;
    private final MutableLiveData<Integer> _phaseIndexFromService; // Реальный индекс от сервиса

    public final LiveData<Integer> sessionCountLiveData;

    private PomodoroTimerService.PomodoroBinder pomodoroBinder = null;
    private final MutableLiveData<Boolean> _serviceBoundLiveData;
    private final AtomicBoolean isBindingOperationInProgress = new AtomicBoolean(false);
    private final AtomicBoolean isUnbindingOperationInProgress = new AtomicBoolean(false);
    private final AtomicBoolean startStopInProgress = new AtomicBoolean(false);


    private Observer<TimerState> timerStateObserverFromService;
    private Observer<List<PomodoroPhase>> phasesObserverFromService;
    private Observer<Integer> phaseIndexObserverFromService;
    private final Observer<PomodoroSettings> settingsObserver;
    private final Observer<List<Task>> upcomingTasksRepoObserver;

    private final SingleLiveEvent<String> _snackbarMessageEventInternal = new SingleLiveEvent<>();
    public final LiveData<String> snackbarMessageEvent = _snackbarMessageEventInternal;

    private final SingleLiveEvent<Boolean> _navigateToSettingsEvent = new SingleLiveEvent<>();
    public final LiveData<Boolean> navigateToSettingsEvent = _navigateToSettingsEvent;

    private final MediatorLiveData<Object> triggerForUiCombination = new MediatorLiveData<>();


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
            @IODispatcher Executor ioExecutor, Logger logger, SnackbarManager snackbarManager) {
        this.taskRepository = taskRepository;
        this.pomodoroManager = pomodoroManager;
        this.settingsRepository = settingsRepository;
        this.pomodoroCycleGenerator = pomodoroCycleGenerator;
        this.forceCompleteTaskWithPomodoroUseCase = forceCompleteTaskWithPomodoroUseCase;
        this.savedStateHandle = savedStateHandle;
        this.applicationContext = context;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.snackbarManager = snackbarManager;

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
        _currentPhaseIndexUiLiveData = new MutableLiveData<>(-1);
        currentPhaseIndexFlow = _currentPhaseIndexUiLiveData;
        _serviceBoundLiveData = new MutableLiveData<>(false);

        _timerStateFromService = new MutableLiveData<>(TimerState.Idle.getInstance());
        _phasesFromService = new MutableLiveData<>(Collections.emptyList());
        _phaseIndexFromService = new MutableLiveData<>(-1);

        sessionCountLiveData = Transformations.switchMap(_currentTaskLiveData, task ->
                (task == null || task.getId() == 0) ? new MutableLiveData<>(0) : pomodoroSessionRepository.getCompletedSessionsCount(task.getId())
        );

        timerStateObserverFromService = engineState -> {
            if (engineState != null) {
                _timerStateFromService.postValue(engineState);
            }
        };
        phasesObserverFromService = phases -> {
            _phasesFromService.postValue(phases != null ? new ArrayList<>(phases) : Collections.emptyList());
        };
        phaseIndexObserverFromService = index -> {
            _phaseIndexFromService.postValue(index != null ? index : -1);
        };
        settingsObserver = newSettings -> {
            PomodoroUiState currentUi = _uiStateLiveData.getValue();
            if (currentUi != null && newSettings != null && !Objects.equals(currentUi.pomodoroSettings, newSettings)) {
                boolean wasSetupMode = currentUi.isTimeSetupMode;
                int newHours = currentUi.estimatedHours;
                int newMinutes = currentUi.estimatedMinutes;
                if (wasSetupMode) {
                    newHours = newSettings.getWorkDurationMinutes() / 60;
                    newMinutes = newSettings.getWorkDurationMinutes() % 60;
                }
                final int finalNewHours = newHours;
                final int finalNewMinutes = newMinutes;
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
            if (currentTaskVal == null && tasks != null && !tasks.isEmpty() && (currentUi == null || currentUi.isTimeSetupMode)) {
                Task newCurrent = tasks.get(0);
                _currentTaskLiveData.postValue(newCurrent);
            }
        };

        settingsRepository.getSettingsFlow().observeForever(settingsObserver);
        taskRepository.getUpcomingTasks().observeForever(upcomingTasksRepoObserver);

        triggerForUiCombination.addSource(_timerStateFromService, value -> triggerForUiCombination.setValue(new Object()));
        triggerForUiCombination.addSource(_phasesFromService, value -> triggerForUiCombination.setValue(new Object()));
        triggerForUiCombination.addSource(_phaseIndexFromService, value -> triggerForUiCombination.setValue(new Object()));
        triggerForUiCombination.addSource(_currentTaskLiveData, value -> triggerForUiCombination.setValue(new Object()));
        triggerForUiCombination.addSource(_generatedPhasesLiveData, value -> triggerForUiCombination.setValue(new Object()));
        triggerForUiCombination.addSource(settingsRepository.getSettingsFlow(), value -> triggerForUiCombination.setValue(new Object()));
        triggerForUiCombination.observeForever(ignored -> combineAndPostUiState());

        loadInitialTaskFromSavedState(savedStateHandle);
        bindToService();
    }

    private void loadInitialTaskFromSavedState(SavedStateHandle handle) {
        updateUiState(s -> s.copy(null, null, null, null, null, null, null, null, null, null, null, null, true, true, true));
        ListenableFuture<PomodoroSettings> settingsFuture = settingsRepository.getSettings();
        Futures.addCallback(settingsFuture, new FutureCallback<PomodoroSettings>() {
            @Override
            public void onSuccess(@Nullable PomodoroSettings initialSettingsLoaded) {
                final PomodoroSettings settingsToUse = (initialSettingsLoaded != null) ? initialSettingsLoaded : new PomodoroSettings();
                ioExecutor.execute(() -> {
                    try {
                        Long taskId = null;
                        Object taskIdObject = handle.get("taskId");
                        if (taskIdObject instanceof Long) {
                            taskId = (Long) taskIdObject;
                        } else if (taskIdObject instanceof String) {
                            try { taskId = Long.parseLong((String) taskIdObject); }
                            catch (NumberFormatException e) { logger.warn(TAG, "Could not parse taskId from String: " + taskIdObject); }
                        }
                        logger.debug(TAG, "loadInitialTaskFromSavedState - Parsed TaskId: " + taskId);

                        if (taskId != null && taskId != -1L) {
                            Task initialTask = Futures.getDone(taskRepository.getTaskById(taskId));
                            if (initialTask != null) {
                                _currentTaskLiveData.postValue(initialTask);
                                logger.info(TAG, "Initial task loaded: ID " + taskId);
                            } else {
                                logger.warn(TAG, "Task with ID " + taskId + " not found. Using defaults.");
                                initializeDefaultUiState(settingsToUse);
                            }
                        } else {
                            logger.debug(TAG, "No taskId. Initializing with default settings.");
                            initializeDefaultUiState(settingsToUse);
                        }
                    } catch (Exception e) {
                        logger.error(TAG, "Error in ioExecutor block of loadInitialTaskFromSavedState", e);
                        initializeDefaultUiState(new PomodoroSettings());
                        updateUiState(s -> s.copy(null,null, null, "Ошибка загрузки задачи",null,null,null,null,null,null,null,null,false, false, true));
                    } finally {
                        updateUiState(s -> s.copy(null, null, null, null, null, null, null, null, null, null, null, null, false, false, false));
                    }
                });
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to load initial settings for PomodoroViewModel", t);
                initializeDefaultUiState(new PomodoroSettings());
                updateUiState(s -> s.copy(null,null,null, "Ошибка загрузки настроек",null,null,null,null,null,null,null,null, false, false, true));
            }
        }, MoreExecutors.directExecutor());
    }

    private void initializeDefaultUiState(PomodoroSettings settings) {
        int defaultMinutes = settings.getWorkDurationMinutes();
        updateUiState(s -> new PomodoroUiState(
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
            if(isBindingOperationInProgress.get() && _serviceBoundLiveData.getValue() == Boolean.FALSE) isBindingOperationInProgress.set(false);
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
        if (_serviceBoundLiveData.getValue() == Boolean.TRUE && pomodoroBinder != null) {
            if (!isUnbindingOperationInProgress.compareAndSet(false, true)) {
                logger.debug(TAG, "Unbinding already in progress.");
                return;
            }
            logger.debug(TAG, "Unbinding from PomodoroTimerService...");
            try {
                unsubscribeFromServiceState();
                applicationContext.unbindService(serviceConnection);
            } catch (Exception e) {
                logger.error(TAG, "Error unbinding from service", e);
            } finally {
                pomodoroBinder = null;
                _serviceBoundLiveData.postValue(false);
                isUnbindingOperationInProgress.set(false);
                logger.info(TAG, "Service unbound handling finished.");
                PomodoroUiState currentUi = _uiStateLiveData.getValue();
                resetToIdleSetupState(currentUi != null ? currentUi.pomodoroSettings : new PomodoroSettings());
            }
        } else {
            logger.debug(TAG, "Service not bound or binder is null. isBound: " + _serviceBoundLiveData.getValue());
        }
    }
    private void subscribeToServiceState() {
        if (pomodoroBinder == null) {
            logger.warn(TAG, "Cannot subscribe to service state: binder is null.");
            return;
        }
        unsubscribeFromServiceState();

        pomodoroBinder.getTimerStateLiveData().observeForever(timerStateObserverFromService);
        pomodoroBinder.getPomodoroPhasesLiveData().observeForever(phasesObserverFromService);
        pomodoroBinder.getCurrentPhaseIndexLiveData().observeForever(phaseIndexObserverFromService);
        logger.debug(TAG, "Subscribed to service state LiveData instances.");
        combineAndPostUiState(); // Запросить обновление UI после подписки
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
        Integer indexFromEngine = _phaseIndexFromService.getValue();
        PomodoroSettings settings = settingsRepository.getSettingsFlow().getValue();
        Task task = _currentTaskLiveData.getValue();
        PomodoroUiState currentSnapshot = _uiStateLiveData.getValue();

        if (engineState == null) engineState = TimerState.Idle.getInstance();
        if (settings == null) settings = new PomodoroSettings();
        if (phasesFromEngine == null) phasesFromEngine = Collections.emptyList();
        if (indexFromEngine == null) indexFromEngine = -1;

        boolean newIsTimeSetupMode = (currentSnapshot != null) ? currentSnapshot.isTimeSetupMode : true;
        List<PomodoroPhase> phasesForUi;
        int phaseIndexForUi;
        int estHours = (currentSnapshot != null) ? currentSnapshot.estimatedHours : (settings.getWorkDurationMinutes() / 60);
        int estMinutes = (currentSnapshot != null) ? currentSnapshot.estimatedMinutes : (settings.getWorkDurationMinutes() % 60);

        if (engineState instanceof TimerState.Idle) {
            newIsTimeSetupMode = true;
            phasesForUi = _generatedPhasesLiveData.getValue() != null ? _generatedPhasesLiveData.getValue() : Collections.emptyList();
            phaseIndexForUi = -1;
            if (task != null) {
                int totalMinutes = estHours * 60 + estMinutes;
                List<PomodoroPhase> expectedPhases = (totalMinutes >= PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES)
                        ? pomodoroCycleGenerator.generatePhases(totalMinutes)
                        : Collections.emptyList();
                if (!Objects.equals(_generatedPhasesLiveData.getValue(), expectedPhases)) {
                    _generatedPhasesLiveData.postValue(new ArrayList<>(expectedPhases));
                    phasesForUi = expectedPhases;
                }
            }
        } else {
            newIsTimeSetupMode = false;
            phasesForUi = new ArrayList<>(phasesFromEngine); // Используем копию
            phaseIndexForUi = indexFromEngine;
            if (!Objects.equals(_generatedPhasesLiveData.getValue(), phasesForUi)) {
                _generatedPhasesLiveData.postValue(new ArrayList<>(phasesForUi));
            }
        }
        if (!Objects.equals(_currentPhaseIndexUiLiveData.getValue(), phaseIndexForUi)) {
            _currentPhaseIndexUiLiveData.postValue(phaseIndexForUi);
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
                ", time: " + newState.formattedTime + ", phasesForUi: " + phasesForUi.size() +
                ", phaseIndexForUi: " + phaseIndexForUi + ", setupMode: " + newState.isTimeSetupMode);
    }


    private PomodoroUiState buildNewUiState(
            TimerState timerState, PomodoroSettings settings, String error,
            int estimatedHours, int estimatedMinutes,
            List<PomodoroPhase> currentDisplayPhases, Integer currentDisplayPhaseIndex,
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
            remainingSeconds = 0;
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

        List<PomodoroPhase> phasesToUse = (currentDisplayPhases != null) ? currentDisplayPhases : Collections.emptyList();
        int actualPhaseIndex = (currentDisplayPhaseIndex != null) ? currentDisplayPhaseIndex : -1;

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
        int newHours = current.estimatedHours;
        try {
            if (hoursText != null && !hoursText.isEmpty()) {
                newHours = Integer.parseInt(hoursText.replaceAll("[^0-9]", ""));
            } else if (hoursText != null && hoursText.isEmpty() && current.estimatedHours != 0) {
                newHours = 0;
            }
        } catch (NumberFormatException ignored) {}
        newHours = Math.max(0, Math.min(23, newHours));

        if (newHours != current.estimatedHours) {
            final int finalNewHours = newHours;
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, finalNewHours, s.estimatedMinutes, null,null,null, null, false, false));
            updateGeneratedPhasesForCurrentEstimatedTime(finalNewHours, current.estimatedMinutes, _currentTaskLiveData.getValue());
        }
    }

    public void onEstimatedMinutesChanged(String minutesText) {
        PomodoroUiState current = _uiStateLiveData.getValue();
        if (current == null || !current.isTimeSetupMode) return;
        int newMinutes = current.estimatedMinutes;
        try {
            if (minutesText != null && !minutesText.isEmpty()) {
                newMinutes = Integer.parseInt(minutesText.replaceAll("[^0-9]", ""));
            } else if (minutesText != null && minutesText.isEmpty() && current.estimatedMinutes != 0) {
                newMinutes = 0;
            }
        } catch (NumberFormatException ignored) {}
        newMinutes = Math.max(0, Math.min(59, newMinutes));

        if (newMinutes != current.estimatedMinutes) {
            final int finalNewMinutes = newMinutes;
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, s.estimatedHours, finalNewMinutes, null,null,null, null, false, false));
            updateGeneratedPhasesForCurrentEstimatedTime(current.estimatedHours, finalNewMinutes, _currentTaskLiveData.getValue());
        }
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
        if (currentUi != null && currentUi.isTimeSetupMode && !Objects.equals(_currentPhaseIndexUiLiveData.getValue(), -1)) {
            _currentPhaseIndexUiLiveData.postValue(-1);
        }
        logger.debug(TAG, "Generated phases updated for " + hours + "h " + minutes + "m. Count: " + newPhases.size());
    }

    public void startOrToggleTimer() {
        if (!startStopInProgress.compareAndSet(false, true)) {
            logger.warn(TAG, "Start/Toggle action already in progress, ignoring.");
            new Handler(Looper.getMainLooper()).postDelayed(() -> startStopInProgress.set(false), 300);
            return;
        }
        logger.debug(TAG, "startOrToggleTimer called by UI.");

        PomodoroUiState currentUiState = _uiStateLiveData.getValue();
        Task task = _currentTaskLiveData.getValue();
        if (currentUiState == null) {
            logger.error(TAG, "Cannot start/toggle: currentUiState is null.");
            startStopInProgress.set(false);
            return;
        }

        TimerState engineState = _timerStateFromService.getValue();
        if (engineState == null) engineState = TimerState.Idle.getInstance();

        logger.debug(TAG, "Current EngineState for toggle: " + engineState.getClass().getSimpleName() +
                ", Current UI isTimeSetupMode: " + currentUiState.isTimeSetupMode);

        if (engineState instanceof TimerState.Running) {
            logger.debug(TAG, "Engine is Running -> Sending PAUSE command.");
            pomodoroManager.pauseTimer();
        } else if (engineState instanceof TimerState.Paused) {
            logger.debug(TAG, "Engine is Paused -> Sending RESUME command.");
            pomodoroManager.resumeTimer();
        } else if (engineState instanceof TimerState.Idle) {
            logger.debug(TAG, "Engine is Idle -> Attempting to START new cycle.");
            if (task == null) {
                showErrorMessage("Сначала выберите задачу");
                startStopInProgress.set(false);
                return;
            }
            int totalMinutes = currentUiState.estimatedHours * 60 + currentUiState.estimatedMinutes;
            if (totalMinutes < PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
                showErrorMessage("Минимальное время: " + PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES + " мин.");
                startStopInProgress.set(false);
                return;
            }
            if (currentUiState.showLongTaskWarningDialog) {
                logger.debug(TAG, "Long task warning dialog is pending, not starting timer.");
                startStopInProgress.set(false);
                return;
            }
            if (totalMinutes > MAX_RECOMMENDED_TASK_DURATION_MINUTES && !currentUiState.showLongTaskWarningDialog) {
                updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, true, null, null, false, false));
                startStopInProgress.set(false);
                return;
            }
            proceedWithPomodoroCycleStartInternal(task, totalMinutes);

        } else if (engineState instanceof TimerState.WaitingForConfirmation) {
            logger.debug(TAG, "Engine is WaitingForConfirmation -> Sending CONFIRM command.");
            pomodoroManager.confirmTimerCompletion();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> startStopInProgress.set(false), 300);
    }

    public void proceedWithPomodoroCycleStart() {
        logger.debug(TAG, "Proceeding with Pomodoro cycle start after warning confirmed.");
        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        Task task = _currentTaskLiveData.getValue();
        if (currentUi != null && task != null) {
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, null, false, false)); // Сбрасываем флаг диалога
            proceedWithPomodoroCycleStartInternal(task, currentUi.estimatedHours * 60 + currentUi.estimatedMinutes);
        } else {
            showErrorMessage("Не удалось запустить таймер: нет задачи или состояния.");
        }
    }

    private void proceedWithPomodoroCycleStartInternal(@NonNull Task taskToUse, int totalMinutesToUse) {
        logger.info(TAG, "proceedWithPomodoroCycleStartInternal for task: " + taskToUse.getId() + ", totalMinutes: " + totalMinutesToUse);
        if (totalMinutesToUse < PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
            showErrorMessage("Время слишком мало для запуска.");
            return;
        }

        List<PomodoroPhase> phases = pomodoroCycleGenerator.generatePhases(totalMinutesToUse);
        if (phases.isEmpty()) {
            showErrorMessage("Не удалось создать план сессии.");
            return;
        }

        // Устанавливаем isTimeSetupMode в false ПЕРЕД отправкой команды в сервис
        updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, false, false, false));
        // Обновляем _generatedPhasesLiveData, чтобы UI сразу показал корректный план
        _generatedPhasesLiveData.postValue(new ArrayList<>(phases));
        // _currentPhaseIndexUiLiveData будет обновлен из _phaseIndexFromService, когда сервис начнет первую фазу
        pomodoroManager.startPomodoroCycle(taskToUse.getId(), taskToUse.getUserId(), new ArrayList<>(phases));
    }

    public void setCurrentTask(Task task) {
        Task previousTask = _currentTaskLiveData.getValue();
        _currentTaskLiveData.postValue(task);
        _showTaskSelectorLiveData.postValue(false);

        TimerState currentEngineState = _timerStateFromService.getValue();
        if (currentEngineState == null) currentEngineState = TimerState.Idle.getInstance();
        PomodoroUiState currentUi = _uiStateLiveData.getValue();

        if (currentUi != null) {
            if (!(currentEngineState instanceof TimerState.Idle) && (previousTask != null && !Objects.equals(previousTask.getId(), task.getId()))) {
                logger.info(TAG, "Task changed while timer was active for task " + previousTask.getId() + ". Stopping timer.");
                pomodoroManager.stopTimer(); // Это асинхронно, TimerEngine перейдет в Idle
            }
            // Сброс в режим настройки всегда при смене задачи или если задача null
            resetToIdleSetupState(currentUi.pomodoroSettings);
            // Обновление сгенерированных фаз для новой задачи (или пустых, если task == null)
            updateGeneratedPhasesForCurrentEstimatedTime(currentUi.estimatedHours, currentUi.estimatedMinutes, task);
        }
    }

    private void resetToIdleSetupState(PomodoroSettings settingsToUse) {
        PomodoroSettings currentSettings = (settingsToUse != null) ? settingsToUse : new PomodoroSettings();
        int defaultMinutes = currentSettings.getWorkDurationMinutes();
        int defaultHours = defaultMinutes / 60;
        int finalDefaultMinutes = defaultMinutes % 60;

        updateUiState(s -> new PomodoroUiState(
                TimerState.Idle.getInstance(),
                pomodoroManager.formatTime(defaultMinutes * 60),
                0f, null, SessionType.FOCUS, 0, 0,
                defaultHours, finalDefaultMinutes, currentSettings,
                false, false, true
        ));
        // При сбросе в Idle, _currentPhaseIndexUiLiveData тоже должен быть -1
        if (!Objects.equals(_currentPhaseIndexUiLiveData.getValue(), -1)) {
            _currentPhaseIndexUiLiveData.postValue(-1);
        }
        // _generatedPhasesLiveData будет обновлен в updateGeneratedPhasesForCurrentEstimatedTime
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

        updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, null, true, null, false, true));

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
                    // isCompletingTaskEarly будет сброшен через combineAndPostUiState -> resetToIdleSetupState
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


    public void navigateToSettings() {
        _navigateToSettingsEvent.setValue(true);
    }
    public void clearNavigateToSettings() {
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
        if (phases == null || phases.isEmpty() || totalFocus == 0) return 0;

        if (timerState instanceof TimerState.Idle) {
            return 1; // В режиме Idle и если есть фокусные сессии, показываем "1 / N"
        }
        if (currentIndex < 0 || currentIndex >= phases.size()) {
            return 1; // Неопределенное состояние, возвращаем 1, если не Idle
        }

        PomodoroPhase currentPhase = phases.get(currentIndex);
        if (currentPhase.isFocus()) {
            return currentPhase.getTotalFocusSessionIndex();
        }
        if (currentPhase.isBreak()) {
            if (currentIndex == phases.size() - 1 || !phases.get(currentIndex + 1).isFocus()) {
                return totalFocus; // Последний перерыв или за ним не фокус
            } else {
                return phases.get(currentIndex + 1).getTotalFocusSessionIndex();
            }
        }
        return 0;
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
        unbindFromService();
        if (settingsObserver != null && settingsRepository != null && settingsRepository.getSettingsFlow() != null) {
            settingsRepository.getSettingsFlow().removeObserver(settingsObserver);
        }
        if (upcomingTasksRepoObserver != null && taskRepository != null && taskRepository.getUpcomingTasks() != null) {
            taskRepository.getUpcomingTasks().removeObserver(upcomingTasksRepoObserver);
        }
        triggerForUiCombination.removeObserver(ignored -> {});
        logger.debug(TAG, "ViewModel cleared and resources released.");
    }
}