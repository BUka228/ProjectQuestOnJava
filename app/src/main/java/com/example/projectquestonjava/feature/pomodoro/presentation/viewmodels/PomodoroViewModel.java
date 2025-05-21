package com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.utils.Logger;
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
import java.util.concurrent.atomic.AtomicBoolean; // Для isBindingOperationInProgress

import javax.inject.Inject;

class PomodoroUiState {
    public final TimerState timerState;
    public final String formattedTime;
    public final float progress;
    public final String errorMessage;
    public final SessionType currentPhaseType;
    public final int totalFocusSessionsInTask;
    public final int currentFocusSessionDisplayIndex;
    public final int estimatedHours;
    public final int estimatedMinutes;
    public final PomodoroSettings pomodoroSettings;
    public final boolean showLongTaskWarningDialog;
    public final boolean isCompletingTaskEarly;
    public final boolean isTimeSetupMode;

    public PomodoroUiState(TimerState timerState, String formattedTime, float progress, String errorMessage,
                           SessionType currentPhaseType, int totalFocusSessionsInTask, int currentFocusSessionDisplayIndex,
                           int estimatedHours, int estimatedMinutes, PomodoroSettings pomodoroSettings,
                           boolean showLongTaskWarningDialog, boolean isCompletingTaskEarly, boolean isTimeSetupMode) {
        this.timerState = timerState != null ? timerState : TimerState.Idle.getInstance();
        this.formattedTime = formattedTime != null ? formattedTime : "00:00";
        this.progress = progress;
        this.errorMessage = errorMessage;
        this.currentPhaseType = currentPhaseType != null ? currentPhaseType : SessionType.FOCUS;
        this.totalFocusSessionsInTask = totalFocusSessionsInTask;
        this.currentFocusSessionDisplayIndex = currentFocusSessionDisplayIndex;
        this.estimatedHours = estimatedHours;
        this.estimatedMinutes = estimatedMinutes;
        this.pomodoroSettings = pomodoroSettings != null ? pomodoroSettings : new PomodoroSettings();
        this.showLongTaskWarningDialog = showLongTaskWarningDialog;
        this.isCompletingTaskEarly = isCompletingTaskEarly;
        this.isTimeSetupMode = isTimeSetupMode;
    }
    // Конструктор по умолчанию
    public PomodoroUiState() {
        this(TimerState.Idle.getInstance(), "00:00", 0f, null, SessionType.FOCUS, 0,0,0, PomodoroCycleGenerator.FOCUS_DURATION_MINUTES, new PomodoroSettings(), false, false, true);
    }
    public PomodoroUiState copy(
            TimerState timerState, String formattedTime, Float progress, String errorMessage,
            SessionType currentPhaseType, Integer totalFocusSessionsInTask, Integer currentFocusSessionDisplayIndex,
            Integer estimatedHours, Integer estimatedMinutes, PomodoroSettings pomodoroSettings,
            Boolean showLongTaskWarningDialog, Boolean isCompletingTaskEarly, Boolean isTimeSetupMode
    ) {
        return new PomodoroUiState(
                timerState != null ? timerState : this.timerState,
                formattedTime != null ? formattedTime : this.formattedTime,
                progress != null ? progress : this.progress,
                errorMessage, // может быть null
                currentPhaseType != null ? currentPhaseType : this.currentPhaseType,
                totalFocusSessionsInTask != null ? totalFocusSessionsInTask : this.totalFocusSessionsInTask,
                currentFocusSessionDisplayIndex != null ? currentFocusSessionDisplayIndex : this.currentFocusSessionDisplayIndex,
                estimatedHours != null ? estimatedHours : this.estimatedHours,
                estimatedMinutes != null ? estimatedMinutes : this.estimatedMinutes,
                pomodoroSettings != null ? pomodoroSettings : this.pomodoroSettings,
                showLongTaskWarningDialog != null ? showLongTaskWarningDialog : this.showLongTaskWarningDialog,
                isCompletingTaskEarly != null ? isCompletingTaskEarly : this.isCompletingTaskEarly,
                isTimeSetupMode != null ? isTimeSetupMode : this.isTimeSetupMode
        );
    }
}


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

    private final MutableLiveData<PomodoroUiState> _uiStateLiveData = new MutableLiveData<>(new PomodoroUiState());
    public LiveData<PomodoroUiState> uiStateLiveData = _uiStateLiveData;

    private final MutableLiveData<Task> _currentTaskLiveData = new MutableLiveData<>(null);
    public LiveData<Task> currentTaskLiveData = _currentTaskLiveData;

    private final MutableLiveData<List<Task>> _upcomingTasksLiveData = new MutableLiveData<>(Collections.emptyList());
    public LiveData<List<Task>> upcomingTasksLiveData = _upcomingTasksLiveData;

    private final MutableLiveData<Boolean> _showTaskSelectorLiveData = new MutableLiveData<>(false);
    public LiveData<Boolean> showTaskSelectorLiveData = _showTaskSelectorLiveData;

    // Для хранения фаз, полученных от сервиса или сгенерированных локально
    private final MutableLiveData<List<PomodoroPhase>> _generatedPhasesLiveData = new MutableLiveData<>(Collections.emptyList());
    public LiveData<List<PomodoroPhase>> generatedPhasesLiveData = _generatedPhasesLiveData;

    private final MutableLiveData<Integer> _currentPhaseIndexLiveData = new MutableLiveData<>(-1);
    public LiveData<Integer> currentPhaseIndexLiveData = _currentPhaseIndexLiveData;

    public final LiveData<Integer> sessionCountLiveData;


    private PomodoroTimerService.PomodoroBinder pomodoroBinder = null;
    private final MutableLiveData<Boolean> _serviceBoundLiveData = new MutableLiveData<>(false);
    // private LiveData<Boolean> serviceBoundLiveData = _serviceBoundLiveData; // Не используется напрямую в UI, но для внутренней логики

    private final AtomicBoolean isBindingOperationInProgress = new AtomicBoolean(false);


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pomodoroBinder = (PomodoroTimerService.PomodoroBinder) service;
            _serviceBoundLiveData.postValue(true);
            isBindingOperationInProgress.set(false);
            logger.debug(TAG, "Service connected.");
            subscribeToServiceState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logger.debug(TAG, "Service disconnected.");
            unsubscribeFromServiceState(); // Отписываемся перед сбросом
            pomodoroBinder = null;
            _serviceBoundLiveData.postValue(false);
            isBindingOperationInProgress.set(false);
            resetToIdleSetupState(_uiStateLiveData.getValue() != null ? _uiStateLiveData.getValue().pomodoroSettings : new PomodoroSettings());
        }
    };

    private Observer<TimerState> timerStateObserverFromService;
    private Observer<List<PomodoroPhase>> phasesObserverFromService;
    private Observer<Integer> phaseIndexObserverFromService;

    @Inject
    public PomodoroViewModel(
            TaskRepository taskRepository, PomodoroManager pomodoroManager,
            PomodoroSessionRepository pomodoroSessionRepository, PomodoroSettingsRepository settingsRepository,
            PomodoroCycleGenerator pomodoroCycleGenerator, ForceCompleteTaskWithPomodoroUseCase forceCompleteTaskWithPomodoroUseCase,
            SavedStateHandle savedStateHandle, @ApplicationContext Context context,
            @IODispatcher Executor ioExecutor, Logger logger) {
        this.taskRepository = taskRepository;
        this.pomodoroManager = pomodoroManager;
        this.settingsRepository = settingsRepository;
        this.pomodoroCycleGenerator = pomodoroCycleGenerator;
        this.forceCompleteTaskWithPomodoroUseCase = forceCompleteTaskWithPomodoroUseCase;
        this.savedStateHandle = savedStateHandle;
        this.applicationContext = context;
        this.ioExecutor = ioExecutor;
        this.logger = logger;

        sessionCountLiveData = Transformations.switchMap(_currentTaskLiveData, task ->
                (task == null || task.getId() == 0) ? new MutableLiveData<>(0) : pomodoroSessionRepository.getCompletedSessionsCount(task.getId())
        );

        loadInitialData();
        bindToService(); // Начинаем привязку при инициализации
    }

    private void loadInitialData() {
        ioExecutor.execute(() -> {
            try {
                PomodoroSettings initialSettings = Futures.getDone(settingsRepository.getSettings());
                String taskIdString = savedStateHandle.get("taskId");
                Long taskId = (taskIdString != null) ? Long.parseLong(taskIdString) : null;

                Task initialTask = null;
                if (taskId != null) {
                    initialTask = Futures.getDone(taskRepository.getTaskById(taskId));
                }

                List<Task> upcoming = new ArrayList<>(); // Загрузка upcomingTasks
                LiveData<List<Task>> upcomingLiveData = taskRepository.getUpcomingTasks();
                // Мы не можем здесь напрямую получить значение из LiveData без observe.
                // Лучше подписаться на upcomingTasksLiveData в конструкторе и обновлять _upcomingTasksLiveData.
                // Пока оставим пустым.
                _upcomingTasksLiveData.postValue(upcoming);


                final Task finalInitialTask = initialTask;
                _uiStateLiveData.postValue(new PomodoroUiState(
                        TimerState.Idle.getInstance(),
                        pomodoroManager.formatTime(initialSettings.getWorkDurationMinutes() * 60),
                        0f, null, SessionType.FOCUS, 0,0,
                        initialSettings.getWorkDurationMinutes() / 60,
                        initialSettings.getWorkDurationMinutes() % 60,
                        initialSettings, false, false, true
                ));
                if (finalInitialTask != null) {
                    _currentTaskLiveData.postValue(finalInitialTask);
                    updateGeneratedPhasesForCurrentEstimatedTime(Objects.requireNonNull(_uiStateLiveData.getValue()).estimatedHours, _uiStateLiveData.getValue().estimatedMinutes, finalInitialTask);
                } else if (!upcoming.isEmpty()){
                    _currentTaskLiveData.postValue(upcoming.get(0)); // Берем первую из upcoming, если задачи не передано
                    updateGeneratedPhasesForCurrentEstimatedTime(Objects.requireNonNull(_uiStateLiveData.getValue()).estimatedHours, _uiStateLiveData.getValue().estimatedMinutes, upcoming.get(0));
                }


            } catch (Exception e) {
                logger.error(TAG, "Error during initial data load", e);
                _uiStateLiveData.postValue(Objects.requireNonNull(_uiStateLiveData.getValue()).copy(null,null,null, "Ошибка загрузки данных", null, null, null, null, null, null, null, null, null));
            }
        });
        // Подписка на LiveData предстоящих задач
        taskRepository.getUpcomingTasks().observeForever(tasks -> { // ВАЖНО: отписаться в onCleared
            _upcomingTasksLiveData.postValue(tasks != null ? tasks : Collections.emptyList());
            if (_currentTaskLiveData.getValue() == null && tasks != null && !tasks.isEmpty()) {
                Task newCurrent = tasks.get(0);
                _currentTaskLiveData.postValue(newCurrent);
                PomodoroUiState ui = _uiStateLiveData.getValue();
                if (ui != null && ui.isTimeSetupMode) {
                    updateGeneratedPhasesForCurrentEstimatedTime(ui.estimatedHours, ui.estimatedMinutes, newCurrent);
                }
            }
        });

    }

    private void bindToService() {
        if (_serviceBoundLiveData.getValue() == Boolean.TRUE || isBindingOperationInProgress.get()) return;
        isBindingOperationInProgress.set(true);
        logger.debug(TAG, "Attempting to bind to PomodoroTimerService...");
        Intent serviceIntent = new Intent(applicationContext, PomodoroTimerService.class);
        try {
            boolean bound = applicationContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
            if (!bound) {
                isBindingOperationInProgress.set(false);
                logger.error(TAG, "bindService returned false.");
                showErrorMessage("Не удалось подключиться к службе таймера.");
            }
        } catch (Exception e) {
            isBindingOperationInProgress.set(false);
            logger.error(TAG, "Exception during bindService", e);
            showErrorMessage("Ошибка подключения к службе таймера.");
        }
    }

    private void unbindFromService() {
        if (_serviceBoundLiveData.getValue() == Boolean.TRUE && pomodoroBinder != null) {
            logger.debug(TAG, "Unbinding from PomodoroTimerService...");
            try {
                applicationContext.unbindService(serviceConnection);
            } catch (Exception e) {
                logger.error(TAG, "Error unbinding from service", e);
            }
            pomodoroBinder = null;
            _serviceBoundLiveData.setValue(false);
        }
        isBindingOperationInProgress.set(false);
    }

    private void subscribeToServiceState() {
        if (pomodoroBinder == null) return;
        unsubscribeFromServiceState(); // Отписываемся от старых, если были

        timerStateObserverFromService = engineState -> {
            if (engineState == null) return;
            PomodoroUiState currentUi = _uiStateLiveData.getValue();
            if (currentUi == null) currentUi = new PomodoroUiState(); // Инициализация, если null

            boolean newIsTimeSetupMode;
            if (engineState instanceof TimerState.Idle && !(currentUi.timerState instanceof TimerState.Idle)) {
                newIsTimeSetupMode = true;
                // Сбрасываем estimatedTime на дефолт из настроек
                PomodoroSettings settings = currentUi.pomodoroSettings;
                int defaultMinutes = settings.getWorkDurationMinutes();
                updateUiState(s -> s.copy(null,null,null,null,null,null,null, defaultMinutes/60, defaultMinutes%60, null,null,null, true));
            } else if (!(engineState instanceof TimerState.Idle)) {
                newIsTimeSetupMode = false;
            } else {
                newIsTimeSetupMode = currentUi.isTimeSetupMode;
            }
            // Обновляем UI на основе состояния движка
            updateUiState(s -> buildNewUiState(engineState, s.pomodoroSettings, s.errorMessage, s.estimatedHours, s.estimatedMinutes,
                    _generatedPhasesLiveData.getValue(), _currentPhaseIndexLiveData.getValue(),
                    s.showLongTaskWarningDialog, s.isCompletingTaskEarly, newIsTimeSetupMode));
        };

        phasesObserverFromService = phases -> {
            _generatedPhasesLiveData.postValue(phases != null ? phases : Collections.emptyList());
            // Перестраиваем UI, так как фазы могли измениться
            updateUiState(s -> buildNewUiState(s.timerState, s.pomodoroSettings, s.errorMessage, s.estimatedHours, s.estimatedMinutes,
                    phases, _currentPhaseIndexLiveData.getValue(),
                    s.showLongTaskWarningDialog, s.isCompletingTaskEarly, s.isTimeSetupMode));
        };
        phaseIndexObserverFromService = index -> {
            _currentPhaseIndexLiveData.postValue(index != null ? index : -1);
            updateUiState(s -> buildNewUiState(s.timerState, s.pomodoroSettings, s.errorMessage, s.estimatedHours, s.estimatedMinutes,
                    _generatedPhasesLiveData.getValue(), index,
                    s.showLongTaskWarningDialog, s.isCompletingTaskEarly, s.isTimeSetupMode));
        };

        pomodoroBinder.getTimerStateLiveData().observeForever(timerStateObserverFromService);
        pomodoroBinder.getPomodoroPhasesLiveData().observeForever(phasesObserverFromService);
        pomodoroBinder.getCurrentPhaseIndexLiveData().observeForever(phaseIndexObserverFromService);
        logger.debug(TAG, "Subscribed to service state LiveData.");
    }

    private void unsubscribeFromServiceState() {
        if (pomodoroBinder != null) {
            if (timerStateObserverFromService != null) pomodoroBinder.getTimerStateLiveData().removeObserver(timerStateObserverFromService);
            if (phasesObserverFromService != null) pomodoroBinder.getPomodoroPhasesLiveData().removeObserver(phasesObserverFromService);
            if (phaseIndexObserverFromService != null) pomodoroBinder.getCurrentPhaseIndexLiveData().removeObserver(phaseIndexObserverFromService);
            logger.debug(TAG, "Unsubscribed from service state LiveData.");
        }
    }

    // Остальные методы (onEstimatedHoursChanged, onEstimatedMinutesChanged, startOrToggleTimer и т.д.)
    // будут использовать pomodoroManager для отправки команд в сервис
    // и _uiStateLiveData для обновления UI напрямую, если это локальные изменения (например, estimatedTime).

    public void onEstimatedHoursChanged(String hoursText) {
        PomodoroUiState current = _uiStateLiveData.getValue();
        if (current == null || !current.isTimeSetupMode) return;
        int newHours = 0;
        try { newHours = Integer.parseInt(hoursText.replaceAll("[^0-9]", "")); } catch (NumberFormatException ignored) {}
        newHours = Math.max(0, Math.min(23, newHours)); // Ограничение 0-23
        int finalNewHours = newHours;
        updateUiState(s -> s.copy(null,null,null,null,null,null,null, finalNewHours, null, null,null,null, null));
        updateGeneratedPhasesForCurrentEstimatedTime(newHours, current.estimatedMinutes, _currentTaskLiveData.getValue());
    }

    public void onEstimatedMinutesChanged(String minutesText) {
        PomodoroUiState current = _uiStateLiveData.getValue();
        if (current == null || !current.isTimeSetupMode) return;
        int newMinutes = 0;
        try { newMinutes = Integer.parseInt(minutesText.replaceAll("[^0-9]", "")); } catch (NumberFormatException ignored) {}
        newMinutes = Math.max(0, Math.min(59, newMinutes)); // Ограничение 0-59
        int finalNewMinutes = newMinutes;
        updateUiState(s -> s.copy(null,null,null,null,null,null,null, null, finalNewMinutes, null,null,null, null));
        updateGeneratedPhasesForCurrentEstimatedTime(current.estimatedHours, newMinutes, _currentTaskLiveData.getValue());
    }

    private void updateGeneratedPhasesForCurrentEstimatedTime(int hours, int minutes, @Nullable Task task) {
        if (task != null) {
            int totalMinutes = hours * 60 + minutes;
            _generatedPhasesLiveData.postValue(
                    totalMinutes >= PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES ?
                            pomodoroCycleGenerator.generatePhases(totalMinutes) :
                            Collections.emptyList()
            );
        } else {
            _generatedPhasesLiveData.postValue(Collections.emptyList());
        }
        if (_uiStateLiveData.getValue() != null && _uiStateLiveData.getValue().isTimeSetupMode) {
            _currentPhaseIndexLiveData.postValue(-1);
        }
        logger.debug(TAG, "Phases updated for estimated time: " + hours + "h " + minutes + "m. Count: " + Objects.requireNonNull(_generatedPhasesLiveData.getValue()).size());
    }


    public void startOrToggleTimer() {
        PomodoroUiState currentVal = _uiStateLiveData.getValue();
        Task task = _currentTaskLiveData.getValue();
        if (currentVal == null) return;

        if (task == null && currentVal.timerState instanceof TimerState.Idle) {
            showErrorMessage("Сначала выберите задачу");
            return;
        }

        if (currentVal.timerState instanceof TimerState.Running) {
            pomodoroManager.pauseTimer();
        } else if (currentVal.timerState instanceof TimerState.Paused) {
            pomodoroManager.resumeTimer();
        } else if (currentVal.timerState instanceof TimerState.Idle) {
            int totalMinutes = currentVal.estimatedHours * 60 + currentVal.estimatedMinutes;
            if (totalMinutes < PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
                showErrorMessage("Минимальное время: " + PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES + " мин.");
                return;
            }
            if (currentVal.showLongTaskWarningDialog) return; // Диалог уже показан
            if (totalMinutes > MAX_RECOMMENDED_TASK_DURATION_MINUTES) {
                updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, true, null, null));
                return;
            }
            proceedWithPomodoroCycleStart(task, totalMinutes); // Используем обновленный метод
        } else if (currentVal.timerState instanceof TimerState.WaitingForConfirmation) {
            pomodoroManager.confirmTimerCompletion();
        }
    }

    public void proceedWithPomodoroCycleStart() { // Вызывается из диалога
        proceedWithPomodoroCycleStart(_currentTaskLiveData.getValue(), null);
    }

    private void proceedWithPomodoroCycleStart(@Nullable Task taskForCycle, @Nullable Integer estimatedMinutesForCycle) {
        Task taskToUse = (taskForCycle != null) ? taskForCycle : _currentTaskLiveData.getValue();
        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        if (currentUi == null) return;

        int totalMinutesToUse = (estimatedMinutesForCycle != null) ?
                estimatedMinutesForCycle :
                (currentUi.estimatedHours * 60 + currentUi.estimatedMinutes);

        if (taskToUse == null) {
            showErrorMessage("Задача не выбрана.");
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, null));
            return;
        }
        if (totalMinutesToUse < PomodoroCycleGenerator.MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
            showErrorMessage("Время слишком мало.");
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, null));
            return;
        }

        List<PomodoroPhase> phases = pomodoroCycleGenerator.generatePhases(totalMinutesToUse);
        if (phases.isEmpty()) {
            showErrorMessage("Не удалось разбить задачу.");
            updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, null));
            return;
        }

        // Сохраняем сгенерированные фазы и сбрасываем индекс
        _generatedPhasesLiveData.postValue(phases);
        _currentPhaseIndexLiveData.postValue(0); // Стартуем с первой фазы

        updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false, null, false)); // isTimeSetupMode = false
        pomodoroManager.startPomodoroCycle(taskToUse.getId(), taskToUse.getUserId(), new ArrayList<>(phases));
    }


    private PomodoroUiState buildNewUiState(
            TimerState timerState, PomodoroSettings settings, String error,
            int estimatedHours, int estimatedMinutes,
            List<PomodoroPhase> currentGeneratedPhases, Integer currentPhaseIndex,
            boolean showLongTaskWarningDialog, boolean isCompletingTaskEarly,
            boolean isTimeSetupMode
    ) {
        if (timerState == null) timerState = TimerState.Idle.getInstance();
        if (settings == null) settings = new PomodoroSettings();
        if (currentGeneratedPhases == null) currentGeneratedPhases = Collections.emptyList();
        if (currentPhaseIndex == null) currentPhaseIndex = -1;


        int remainingSeconds = 0;
        int totalSecondsInCurrentPhase = 0;
        SessionType currentPhaseTypeFromState = SessionType.FOCUS; // По умолчанию

        if (timerState instanceof TimerState.Running running) {
            remainingSeconds = running.getRemainingSeconds();
            totalSecondsInCurrentPhase = running.getTotalSeconds();
            currentPhaseTypeFromState = running.getType();
        } else if (timerState instanceof TimerState.Paused paused) {
            remainingSeconds = paused.getRemainingSeconds();
            totalSecondsInCurrentPhase = paused.getTotalSeconds();
            currentPhaseTypeFromState = paused.getType();
        } else if (timerState instanceof TimerState.WaitingForConfirmation waiting) {
            // Таймер завершен
            totalSecondsInCurrentPhase = waiting.getTotalSeconds();
            currentPhaseTypeFromState = waiting.getType();
        } else if (timerState instanceof TimerState.Idle) {
            int idleDurationMinutes = isTimeSetupMode ? (estimatedHours * 60 + estimatedMinutes) : settings.getWorkDurationMinutes();
            remainingSeconds = Math.max(0, idleDurationMinutes * 60);
            totalSecondsInCurrentPhase = remainingSeconds; // Для Idle progress будет 0
            // По умолчанию для Idle
        }

        float progress = 0f;
        if (totalSecondsInCurrentPhase > 0 && !(timerState instanceof TimerState.Idle)) {
            progress = 1f - ((float) remainingSeconds / totalSecondsInCurrentPhase);
        } else if (timerState instanceof TimerState.WaitingForConfirmation) {
            progress = 1f;
        }
        progress = Math.max(0f, Math.min(1f, progress)); // Ограничиваем 0..1

        int totalFocusSessions = (int) currentGeneratedPhases.stream().filter(PomodoroPhase::isFocus).count();
        int currentFocusSessionDisplayIndex = calculateFocusDisplayIndex(currentGeneratedPhases, currentPhaseIndex, timerState, totalFocusSessions);

        return new PomodoroUiState(
                timerState, pomodoroManager.formatTime(remainingSeconds), progress, error,
                currentPhaseTypeFromState, totalFocusSessions, currentFocusSessionDisplayIndex,
                estimatedHours, estimatedMinutes, settings, showLongTaskWarningDialog, isCompletingTaskEarly,
                isTimeSetupMode
        );
    }

    public void setCurrentTask(Task task) {
        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        Task previousTask = _currentTaskLiveData.getValue();

        _currentTaskLiveData.postValue(task);
        _showTaskSelectorLiveData.postValue(false);

        if (currentUi != null && !(currentUi.timerState instanceof TimerState.Idle) && (previousTask == null || previousTask.getId() != task.getId())) {
            pomodoroManager.stopTimer(); // Это вызовет resetToIdleSetupState через подписку на сервис
        } else {
            // Если таймер уже Idle или задача та же, просто переходим в режим настройки
            resetToIdleSetupState(currentUi != null ? currentUi.pomodoroSettings : new PomodoroSettings());
            // Обновляем фазы для новой задачи, если она выбрана
            if (currentUi != null) {
                updateGeneratedPhasesForCurrentEstimatedTime(currentUi.estimatedHours, currentUi.estimatedMinutes, task);
            }
        }
    }

    private void resetToIdleSetupState(PomodoroSettings settingsToUse) {
        int defaultMinutes = settingsToUse.getWorkDurationMinutes();
        int defaultHours = defaultMinutes / 60;
        int finalDefaultMinutes = defaultMinutes % 60;

        updateUiState(s -> s.copy(
                TimerState.Idle.getInstance(), // Явно ставим Idle
                pomodoroManager.formatTime(defaultMinutes * 60),
                0f, null, SessionType.FOCUS, 0,0,
                defaultHours, finalDefaultMinutes,
                settingsToUse, false, false, true // isTimeSetupMode = true
        ));
        // Сброс фаз и индекса
        _generatedPhasesLiveData.postValue(Collections.emptyList());
        _currentPhaseIndexLiveData.postValue(-1);
        logger.debug(TAG, "ViewModel reset to Idle/Setup mode.");
    }

    public void dismissLongTaskWarningDialog() { updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, false,null, null)); }
    public void skipCurrentBreakAndStartNextFocus() {
        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        List<PomodoroPhase> phases = _generatedPhasesLiveData.getValue();
        Integer currentIndex = _currentPhaseIndexLiveData.getValue();

        if (currentUi != null && phases != null && currentIndex != null &&
                currentIndex >= 0 && currentIndex < phases.size() &&
                phases.get(currentIndex).isBreak() &&
                !(currentUi.timerState instanceof TimerState.Idle)) {
            logger.info(TAG, "User skipping current break. Current index: " + currentIndex);
            pomodoroManager.skipBreak();
        } else {
            logger.warn(TAG, "Skip break invalid. Current phase type: " + (phases != null && currentIndex !=null && currentIndex >=0 && currentIndex < phases.size() ? phases.get(currentIndex).getType() : "N/A") + ", Timer state: " + (currentUi != null ? currentUi.timerState:"N/A"));
        }
    }

    public void completeTaskEarly() {
        Task task = _currentTaskLiveData.getValue();
        PomodoroUiState currentUi = _uiStateLiveData.getValue();
        if (task == null || currentUi == null || currentUi.isCompletingTaskEarly) return;

        updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, null, true, null));
        showErrorMessage(null); // Сбрасываем предыдущие ошибки

        ioExecutor.execute(() -> {
            InterruptedPhaseInfo interruptedInfo = null;
            if (!(currentUi.timerState instanceof TimerState.Idle) && pomodoroBinder != null && _serviceBoundLiveData.getValue() == Boolean.TRUE) {
                try {
                    interruptedInfo = pomodoroBinder.getCurrentInterruptedPhaseInfoFromEngine();
                } catch (Exception e) {
                    logger.error(TAG, "Error getting interrupted phase info from binder", e);
                }
            }

            if (!(currentUi.timerState instanceof TimerState.Idle)) {
                pomodoroManager.stopTimer(); // Отправляем команду на остановку
                // Ждем, пока сервис остановится и ViewModel получит Idle состояние.
                // Это не идеальное решение, лучше использовать коллбэк или другой механизм ожидания.
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }

            final InterruptedPhaseInfo finalInterruptedInfo = interruptedInfo;
            ListenableFuture<Void> completeFuture = forceCompleteTaskWithPomodoroUseCase.execute(task.getId(), finalInterruptedInfo);
            Futures.addCallback(completeFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    _currentTaskLiveData.postValue(null); // Сбрасываем текущую задачу
                    // resetToIdleSetupState вызовется через подписку на сервис, когда он перейдет в Idle
                    showSnackbarMessage("Задача '" + task.getTitle() + "' завершена.");
                    updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, null, false, null));
                }
                @Override
                public void onFailure(@NonNull Throwable t) {
                    showErrorMessage("Ошибка завершения: " + t.getMessage());
                    updateUiState(s -> s.copy(null,null,null,null,null,null,null, null,null,null, null, false, null));
                }
            }, MoreExecutors.directExecutor()); // Коллбэк на том же потоке
        });
    }


    public void stopTimer() { pomodoroManager.stopTimer(); }
    public void toggleTaskSelector() { _showTaskSelectorLiveData.setValue(!Objects.requireNonNull(_showTaskSelectorLiveData.getValue())); }
    public void clearErrorMessage() { updateUiState(s -> s.copy(null,null,null, null, null,null,null, null, null,null, null, null, null)); }

    private void showErrorMessage(String message) {
        updateUiState(s -> s.copy(null,null,null, message, null,null,null, null, null,null, null, null, null));
        // Таймер для скрытия сообщения через 3.5 сек
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            PomodoroUiState current = _uiStateLiveData.getValue();
            if (current != null && Objects.equals(current.errorMessage, message)) {
                clearErrorMessage();
            }
        }, 3500);
    }
    private void showSnackbarMessage(String message) {
        // Используем SnackbarManager, если он был внедрен и предназначен для таких сообщений
        // snackbarManager.showMessage(message);
        // Или обновляем свое состояние, если Snackbar показывается из этого ViewModel
        _uiStateLiveData.postValue(Objects.requireNonNull(_uiStateLiveData.getValue()).copy(null,null, Float.valueOf(message),null,null,null,null,null,null,null,null,null,null));
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            PomodoroUiState current = _uiStateLiveData.getValue();
            if (current != null && Objects.equals(current.errorMessage, message)) {
                _uiStateLiveData.postValue(current.copy(null,null,null,null,null,null,null,null,null,null,null,null,null));
            }
        }, 3000);
    }


    private int calculateFocusDisplayIndex(List<PomodoroPhase> phases, int currentIndex, TimerState timerState, int totalFocus) {
        if (phases == null || phases.isEmpty()) return (totalFocus > 0 && timerState instanceof TimerState.Idle) ? 1 : 0;
        if (currentIndex < 0 || currentIndex >= phases.size()) {
            return totalFocus > 0 && timerState instanceof TimerState.Idle ?
                    Objects.requireNonNull(phases.stream().filter(PomodoroPhase::isFocus).findFirst().orElse(null)).getTotalFocusSessionIndex()
                    : 0;
        }
        PomodoroPhase currentPhase = phases.get(currentIndex);
        if (currentPhase.isFocus()) return currentPhase.getTotalFocusSessionIndex();
        if (currentIndex + 1 < phases.size() && phases.get(currentIndex + 1).isFocus()) {
            return phases.get(currentIndex + 1).getTotalFocusSessionIndex();
        }
        if (currentPhase.isBreak() && totalFocus > 0 &&
                (currentIndex == phases.size() - 1 || (currentIndex + 1 < phases.size() && !phases.get(currentIndex + 1).isFocus()))) {
            return totalFocus; // Показываем общее количество, если это последний перерыв
        }
        return 0;
    }

    private void updateUiState(UiStateUpdaterPomodoro updater) {
        PomodoroUiState current = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.update(current != null ? current : new PomodoroUiState()));
    }

    @FunctionalInterface
    interface UiStateUpdaterPomodoro {
        PomodoroUiState update(PomodoroUiState currentState);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        unsubscribeFromServiceState(); // Отписываемся от LiveData сервиса
        unbindFromService();           // Отвязываемся от сервиса

        // Отписка от LiveData предстоящих задач, если была подписка observeForever
        if (upcomingTasksObserver != null && taskRepository != null) { // Добавим проверку на null
            taskRepository.getUpcomingTasks().removeObserver(upcomingTasksObserver);
        }

        logger.debug(TAG, "ViewModel cleared and unbound from service.");
    }

    private final Observer<List<Task>> upcomingTasksObserver = tasks -> {
        _upcomingTasksLiveData.postValue(tasks != null ? tasks : Collections.emptyList());
        PomodoroUiState currentUi = _uiStateLiveData.getValue(); // Сохраняем для проверки
        Task currentTaskVal = _currentTaskLiveData.getValue();

        if (currentTaskVal == null && tasks != null && !tasks.isEmpty()) {
            Task newCurrent = tasks.get(0);
            _currentTaskLiveData.postValue(newCurrent);
            if (currentUi != null && currentUi.isTimeSetupMode) {
                updateGeneratedPhasesForCurrentEstimatedTime(currentUi.estimatedHours, currentUi.estimatedMinutes, newCurrent);
            }
        }
    };
}