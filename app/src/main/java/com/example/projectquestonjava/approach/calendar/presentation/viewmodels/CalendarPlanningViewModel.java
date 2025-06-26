package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarMonthData;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskWithTagsAndPomodoro;
import com.example.projectquestonjava.approach.calendar.domain.model.PlanningUiState;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskFilterOption;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskSortOption;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarRepository;
import com.example.projectquestonjava.approach.calendar.domain.usecases.GetCalendarMonthDataUseCase;
import com.example.projectquestonjava.approach.calendar.domain.usecases.UpdateCalendarTaskUseCase;
import com.example.projectquestonjava.approach.calendar.extensions.CalendarExtensions;
import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.di.MainExecutor;
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.core.domain.usecases.DeleteTaskUseCase;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;


@HiltViewModel
public class CalendarPlanningViewModel extends ViewModel {

    private static final String TAG = "CalendarPlanningVM";

    private final CalendarRepository calendarRepository;
    private final GetCalendarMonthDataUseCase getCalendarMonthDataUseCase;
    private final UpdateCalendarTaskUseCase updateTaskUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final PriorityResolver priorityResolver;
    private final DateTimeUtils dateTimeUtils;
    private final SnackbarManager snackbarManager;
    private final Logger logger;
    private final Executor ioExecutor;
    private final Executor mainExecutor; // Добавлен mainExecutor

    private final MutableLiveData<PlanningUiState> _uiStateLiveData = new MutableLiveData<>(new PlanningUiState());
    public LiveData<PlanningUiState> uiStateLiveData = _uiStateLiveData;

    private final MutableLiveData<YearMonth> _currentMonthLiveData = new MutableLiveData<>(YearMonth.now());
    public LiveData<YearMonth> currentMonthLiveData = _currentMonthLiveData;

    private final MutableLiveData<LocalDate> _selectedDateLiveData = new MutableLiveData<>(null);
    public LiveData<LocalDate> selectedDateLiveData = _selectedDateLiveData;

    private final MutableLiveData<Boolean> _calendarExpandedLiveData = new MutableLiveData<>(true);
    public LiveData<Boolean> calendarExpandedLiveData = _calendarExpandedLiveData;

    private final MutableLiveData<TaskSortOption> _sortOptionLiveData = new MutableLiveData<>(TaskSortOption.TIME_ASC);
    public LiveData<TaskSortOption> sortOptionLiveData = _sortOptionLiveData;

    private final MutableLiveData<Set<TaskFilterOption>> _filterOptionsLiveData =
            new MutableLiveData<>(Collections.singleton(TaskFilterOption.INCOMPLETE));
    public LiveData<Set<TaskFilterOption>> filterOptionsLiveData = _filterOptionsLiveData;

    private final MutableLiveData<Boolean> _showMoveTaskSheetLiveData = new MutableLiveData<>(false);
    public LiveData<Boolean> showMoveTaskSheetLiveData = _showMoveTaskSheetLiveData;

    private final MutableLiveData<Long> _taskToMoveIdLiveData = new MutableLiveData<>(null);

    private final MutableLiveData<CalendarTaskSummary> _taskDetailsForBottomSheet = new MutableLiveData<>(null);
    public LiveData<CalendarTaskSummary> taskDetailsForBottomSheetLiveData = _taskDetailsForBottomSheet;

    private final LiveData<Long> workspaceIdSourceLiveData;
    private final LiveData<CalendarMonthData> calendarDataLiveData;
    public final LiveData<List<CalendarTaskSummary>> filteredTasksLiveData;
    public final LiveData<Map<LocalDate, Integer>> dailyTaskCountsLiveData;


    @Inject
    public CalendarPlanningViewModel(
            GetCalendarMonthDataUseCase getCalendarMonthDataUseCase,
            CalendarRepository calendarRepository,
            UpdateCalendarTaskUseCase updateTaskUseCase,
            DeleteTaskUseCase deleteTaskUseCase,
            WorkspaceSessionManager workspaceSessionManager,
            PriorityResolver priorityResolver,
            SnackbarManager snackbarManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            @MainExecutor Executor mainExecutor,
            Logger logger) {
        this.getCalendarMonthDataUseCase = getCalendarMonthDataUseCase;
        this.calendarRepository = calendarRepository;
        this.updateTaskUseCase = updateTaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.workspaceSessionManager = workspaceSessionManager;
        this.priorityResolver = priorityResolver;
        this.dateTimeUtils = dateTimeUtils;
        this.snackbarManager = snackbarManager;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutor; // Сохраняем mainExecutor

        logger.info(TAG, "ViewModel initialized. Instance: " + this.hashCode());

        workspaceIdSourceLiveData = this.workspaceSessionManager.getWorkspaceIdLiveData();

        MediatorLiveData<TriggerData> triggerLiveData = new MediatorLiveData<>();

        Observer<Object> commonTriggerObserver = o -> {
            YearMonth month = _currentMonthLiveData.getValue();
            LocalDate date = _selectedDateLiveData.getValue();
            Long wsId = workspaceIdSourceLiveData.getValue();
            if (month != null && wsId != null) {
                triggerLiveData.setValue(new TriggerData(month, date, wsId));
            } else {
                logger.warn(TAG, "commonTriggerObserver: One of the essential triggers (month or wsId) is null. Month: " + month + ", WsId: " + wsId);
            }
        };

        triggerLiveData.addSource(_currentMonthLiveData, commonTriggerObserver);
        triggerLiveData.addSource(_selectedDateLiveData, commonTriggerObserver);
        triggerLiveData.addSource(workspaceIdSourceLiveData, commonTriggerObserver);

        YearMonth initialMonth = _currentMonthLiveData.getValue();
        LocalDate initialDate = _selectedDateLiveData.getValue();
        Long initialWsId = workspaceIdSourceLiveData.getValue();
        if (initialMonth != null && initialWsId != null) {
            triggerLiveData.setValue(new TriggerData(initialMonth, initialDate, initialWsId));
        } else {
            logger.debug(TAG, "Initial trigger values not all set yet. Month: " + initialMonth + ", WsId: " + initialWsId);
        }

        calendarDataLiveData = Transformations.switchMap(triggerLiveData, trigger -> {
            if (trigger == null) {
                logger.warn(TAG, "calendarDataLiveData: Trigger is null. Returning EMPTY.");
                MutableLiveData<CalendarMonthData> emptyData = new MutableLiveData<>();
                emptyData.setValue(CalendarMonthData.EMPTY);
                return emptyData;
            }
            YearMonth month = trigger.month;
            LocalDate date = trigger.date;
            Long workspaceId = trigger.workspaceId;

            logger.debug(TAG, "calendarDataLiveData: Processing Trigger. Month=" + month + ", Date=" + date + ", WsId=" + workspaceId);

            if (workspaceId == null || workspaceId == WorkspaceSessionManager.NO_WORKSPACE_ID) {
                logger.warn(TAG, "calendarDataLiveData: No active workspace (wsId: " + workspaceId + "). Returning EMPTY.");
                MutableLiveData<CalendarMonthData> emptyData = new MutableLiveData<>();
                emptyData.setValue(CalendarMonthData.EMPTY);
                return emptyData;
            }
            updateUiState(s -> s.copyWithLoading(true).copyWithError(null));

            if (date != null) {
                logger.debug(TAG, "calendarDataLiveData: Fetching data for selected date: " + date);
                LiveData<List<CalendarTaskWithTagsAndPomodoro>> tasksForDay = calendarRepository.getTasksForDay(workspaceId, date.atStartOfDay());
                return Transformations.map(tasksForDay, tasksWithDetails -> {
                    updateUiState(s -> s.copyWithLoading(false));
                    if (tasksWithDetails == null) {
                        logger.warn(TAG, "calendarDataLiveData: tasksForDay returned null for date " + date + ". Using EMPTY.");
                        return CalendarMonthData.EMPTY;
                    }
                    logger.debug(TAG, "calendarDataLiveData: Received " + tasksWithDetails.size() + " tasks for date " + date);
                    List<CalendarTaskSummary> summaries = CalendarExtensions.toTaskSummaries(tasksWithDetails, priorityResolver, dateTimeUtils);
                    Map<LocalDate, Integer> counts = Collections.singletonMap(date, summaries.size());
                    return new CalendarMonthData(summaries, counts);
                });
            } else {
                logger.debug(TAG, "calendarDataLiveData: Fetching data for month: " + month);
                LiveData<CalendarMonthData> monthData = getCalendarMonthDataUseCase.execute(month.atDay(1));
                MediatorLiveData<CalendarMonthData> mediatedResult = new MediatorLiveData<>();
                mediatedResult.addSource(monthData, data -> {
                    updateUiState(s -> s.copyWithLoading(false));
                    logger.debug(TAG, "calendarDataLiveData: Received data for month " + month + ". Tasks: " + (data != null ? data.getTasks().size() : "null_data") + ", Counts: " + (data != null ? data.getDailyTaskCounts().size() : "null_counts"));
                    mediatedResult.setValue(data != null ? data : CalendarMonthData.EMPTY);
                });
                return mediatedResult;
            }
        });

        dailyTaskCountsLiveData = Transformations.map(calendarDataLiveData, monthData -> {
            if (monthData == null) return Collections.emptyMap();
            logger.debug(TAG, "dailyTaskCountsLiveData updated with " + monthData.getDailyTaskCounts().size() + " entries.");
            return monthData.getDailyTaskCounts();
        });

        MediatorLiveData<Quartet<List<CalendarTaskSummary>, Set<TaskFilterOption>, TaskSortOption, LocalDate>> filterSortTrigger = new MediatorLiveData<>();
        filterSortTrigger.addSource(Transformations.map(calendarDataLiveData, CalendarMonthData::getTasks), tasks -> {
            logger.debug(TAG, "filterSortTrigger: Tasks updated, count: " + (tasks != null ? tasks.size() : "null"));
            updateFilterSortTriggerValue(filterSortTrigger);
        });
        filterSortTrigger.addSource(_filterOptionsLiveData, filters -> {
            logger.debug(TAG, "filterSortTrigger: Filter options updated: " + filters);
            updateFilterSortTriggerValue(filterSortTrigger);
        });
        filterSortTrigger.addSource(_sortOptionLiveData, sort -> {
            logger.debug(TAG, "filterSortTrigger: Sort option updated: " + sort);
            updateFilterSortTriggerValue(filterSortTrigger);
        });
        filterSortTrigger.addSource(_selectedDateLiveData, date -> {
            logger.debug(TAG, "filterSortTrigger: Selected date updated: " + date);
            updateFilterSortTriggerValue(filterSortTrigger);
        });
        updateFilterSortTriggerValue(filterSortTrigger);

        filteredTasksLiveData = Transformations.map(filterSortTrigger, trigger -> {
            if (trigger == null || trigger.first == null) {
                logger.debug(TAG, "filteredTasksLiveData: Trigger or tasks in trigger is null, returning empty list.");
                return Collections.emptyList();
            }
            return filterAndSortTasks(trigger.first, trigger.fourth, trigger.second, trigger.third);
        });
    }

    private void updateFilterSortTriggerValue(MediatorLiveData<Quartet<List<CalendarTaskSummary>, Set<TaskFilterOption>, TaskSortOption, LocalDate>> mediator) {
        List<CalendarTaskSummary> tasks = calendarDataLiveData.getValue() != null ? calendarDataLiveData.getValue().getTasks() : null;
        Set<TaskFilterOption> filters = _filterOptionsLiveData.getValue();
        TaskSortOption sort = _sortOptionLiveData.getValue();
        LocalDate date = _selectedDateLiveData.getValue();

        if (tasks != null && filters != null && sort != null) {
            logger.debug(TAG, "updateFilterSortTriggerValue: All components ready. Tasks: " + tasks.size() + ", Date: " + date + ", Filters: " + filters + ", Sort: " + sort);
            mediator.setValue(new Quartet<>(tasks, filters, sort, date));
        } else {
            logger.debug(TAG, "updateFilterSortTriggerValue: Not all components ready. Tasks: " + (tasks != null) + ", Filters: " + (filters != null) + ", Sort: " + (sort != null));
        }
    }

    private void updateUiState(UiStateUpdaterPlanning updater) {
        PlanningUiState currentState = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.update(currentState != null ? currentState : new PlanningUiState()));
    }

    @FunctionalInterface
    interface UiStateUpdaterPlanning { PlanningUiState update(PlanningUiState currentState); }

    public void updateMonth(YearMonth month) {
        if (!Objects.equals(_currentMonthLiveData.getValue(), month)) {
            logger.info(TAG, "updateMonth: New month selected: " + month);
            _currentMonthLiveData.setValue(month);
            if (_selectedDateLiveData.getValue() != null) {
                logger.debug(TAG, "updateMonth: Resetting selectedDate as month changed.");
                _selectedDateLiveData.setValue(null);
            }
        }
    }

    public void selectDate(LocalDate date) {
        LocalDate currentSelected = _selectedDateLiveData.getValue();
        LocalDate newSelectedDate = Objects.equals(currentSelected, date) ? null : date;
        logger.info(TAG, "selectDate: User clicked on date. CurrentSelected=" + currentSelected + ", ClickedDate=" + date + ", NewSelectedDate=" + newSelectedDate);
        _selectedDateLiveData.setValue(newSelectedDate);

        if (newSelectedDate != null) {
            YearMonth monthOfNewDate = YearMonth.from(newSelectedDate);
            if (!Objects.equals(monthOfNewDate, _currentMonthLiveData.getValue())) {
                logger.debug(TAG, "selectDate: Month ("+ _currentMonthLiveData.getValue() +") also changed to: " + monthOfNewDate + " due to date selection.");
                _currentMonthLiveData.setValue(monthOfNewDate);
            }
        }
    }

    public void toggleCalendarExpanded() {
        boolean newState = !Objects.requireNonNull(_calendarExpandedLiveData.getValue());
        _calendarExpandedLiveData.setValue(newState);
        logger.debug(TAG, "Calendar expanded toggled to: " + newState);
    }
    public void updateSortOption(TaskSortOption option) {
        logger.debug(TAG, "Updating sort option to: " + option);
        _sortOptionLiveData.setValue(option);
    }
    public void toggleFilterOption(TaskFilterOption option) {
        Set<TaskFilterOption> currentOptions = new HashSet<>(Objects.requireNonNull(_filterOptionsLiveData.getValue()));
        Set<TaskFilterOption> newOptions;
        if (option == TaskFilterOption.ALL) {
            newOptions = Collections.singleton(TaskFilterOption.ALL);
        } else {
            currentOptions.remove(TaskFilterOption.ALL);
            if (currentOptions.contains(option)) {
                currentOptions.remove(option);
            } else {
                currentOptions.add(option);
            }
            newOptions = currentOptions.isEmpty() ? Collections.singleton(TaskFilterOption.INCOMPLETE) : currentOptions;
        }
        logger.debug(TAG, "Updating filter options to: " + newOptions);
        _filterOptionsLiveData.setValue(newOptions);
    }
    public void resetSortAndFilters() {
        logger.debug(TAG, "Resetting sort and filters to default");
        _sortOptionLiveData.setValue(TaskSortOption.TIME_ASC);
        _filterOptionsLiveData.setValue(Collections.singleton(TaskFilterOption.INCOMPLETE));
    }

    public void showTaskDetails(CalendarTaskSummary task) { _taskDetailsForBottomSheet.setValue(task); }
    public void clearTaskDetails() { _taskDetailsForBottomSheet.setValue(null); }
    public void onMoveSheetShown() { _showMoveTaskSheetLiveData.postValue(false); } // Используем postValue
    public void clearError() { updateUiState(s -> s.copy(null, null, null)); }
    public void clearSuccessMessage() { updateUiState(s -> s.copy(null, null, null)); }

    public void deleteTask(long taskId) {
        logger.debug(TAG, "Attempting to delete task: taskId=" + taskId);
        updateUiState(s -> s.copy(true, null, null));
        ListenableFuture<Void> future = deleteTaskUseCase.execute(taskId);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                mainExecutor.execute(() -> { // Переключаемся на главный поток для LiveData
                    logger.info(TAG, "Task " + taskId + " deleted successfully.");
                    updateUiState(s -> s.copy(false, null, "Задача удалена"));
                    refreshCurrentData();
                });
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                mainExecutor.execute(() -> {
                    logger.error(TAG, "Error deleting task " + taskId, t);
                    updateUiState(s -> s.copy(false, "Ошибка удаления: " + t.getMessage(), null));
                });
            }
        }, ioExecutor); // Коллбэк изначально на ioExecutor
    }

    public void requestMoveTask(long taskId) {
        logger.debug(TAG, "Requesting move for task " + taskId);
        _taskToMoveIdLiveData.postValue(taskId);
        _showMoveTaskSheetLiveData.postValue(true);
    }

    public void onMoveDateSelected(LocalDate newDate) {
        _showMoveTaskSheetLiveData.postValue(false);
        Long taskId = _taskToMoveIdLiveData.getValue();
        if (taskId == null) {
            logger.warn(TAG, "onMoveDateSelected: taskToMoveId is null. Move cancelled.");
            onMoveCancelled(); return;
        }
        logger.debug(TAG, "Attempting to move task " + taskId + " to " + newDate);
        updateUiState(s -> s.copy(true, null, null));

        List<CalendarTaskSummary> tasksSource = calendarDataLiveData.getValue() != null ? calendarDataLiveData.getValue().getTasks() : Collections.emptyList();
        if (tasksSource.isEmpty() && filteredTasksLiveData.getValue() != null) {
            tasksSource = filteredTasksLiveData.getValue();
        }

        CalendarTaskSummary taskToMove = tasksSource.stream().filter(t -> t.getId() == taskId).findFirst().orElse(null);

        if (taskToMove == null) {
            logger.error(TAG, "Task " + taskId + " not found in current data for move.");
            updateUiState(s -> s.copy(false, "Задача для перемещения не найдена", null));
            onMoveCancelled(); return;
        }

        LocalDateTime newDateTime = LocalDateTime.of(newDate, taskToMove.getDueDate().toLocalTime());
        TaskInput taskInput = new TaskInput(
                taskToMove.getId(), taskToMove.getTitle(), taskToMove.getDescription(),
                newDateTime, taskToMove.getRecurrenceRule(), new HashSet<>(taskToMove.getTags())
        );

        ListenableFuture<Void> updateFuture = updateTaskUseCase.execute(taskInput);
        final long finalTaskId = taskId;
        final LocalDate originalDate = taskToMove.getDueDate().toLocalDate();
        Futures.addCallback(updateFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                mainExecutor.execute(() -> { // Переключаемся на главный поток
                    logger.info(TAG, "Task " + finalTaskId + " successfully moved to " + newDate + ".");
                    updateUiState(s -> s.copy(false, null, "Задача перемещена на " + newDate.format(DateTimeFormatter.ofPattern("d MMMM", new Locale("ru")))));
                    onMoveCancelled(); // Этот метод тоже должен использовать postValue
                    refreshDataForDate(newDate);
                    if (!Objects.equals(YearMonth.from(newDate), YearMonth.from(originalDate))) {
                        refreshDataForMonth(YearMonth.from(originalDate));
                    } else if (!Objects.equals(newDate, originalDate)) {
                        refreshDataForDate(originalDate);
                    } else if (_selectedDateLiveData.getValue() == null){
                        refreshDataForMonth(_currentMonthLiveData.getValue());
                    }
                });
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                mainExecutor.execute(() -> { // Переключаемся на главный поток
                    logger.error(TAG, "Error moving task " + finalTaskId, t);
                    updateUiState(s -> s.copy(false, "Ошибка перемещения: " + t.getMessage(), null));
                    onMoveCancelled(); // Этот метод тоже должен использовать postValue
                });
            }
        }, ioExecutor); // Коллбэк изначально на ioExecutor
    }

    public void onMoveCancelled() {
        _taskToMoveIdLiveData.postValue(null);
        _showMoveTaskSheetLiveData.postValue(false);
        logger.debug(TAG, "Move cancelled");
    }

    public void refreshCurrentData() {
        LocalDate date = _selectedDateLiveData.getValue();
        YearMonth month = _currentMonthLiveData.getValue();
        logger.info(TAG, "refreshCurrentData called. SelectedDate=" + date + ", CurrentMonth=" + month);
        if (date != null) {
            _selectedDateLiveData.postValue(LocalDate.from(date));
        } else if (month != null) {
            _currentMonthLiveData.postValue(YearMonth.from(month));
        }
    }

    private void refreshDataForDate(LocalDate dateToRefresh) {
        LocalDate currentSelected = _selectedDateLiveData.getValue();
        YearMonth currentMonthVal = _currentMonthLiveData.getValue();
        logger.debug(TAG, "refreshDataForDate: " + dateToRefresh + ". Current selected: " + currentSelected + ", current month: " + currentMonthVal);

        if (Objects.equals(currentSelected, dateToRefresh)) {
            _selectedDateLiveData.postValue(LocalDate.from(dateToRefresh));
        } else if (currentSelected == null && currentMonthVal != null && YearMonth.from(dateToRefresh).equals(currentMonthVal)) {
            _currentMonthLiveData.postValue(YearMonth.from(currentMonthVal));
        }
    }
    private void refreshDataForMonth(YearMonth monthToRefresh) {
        if (_selectedDateLiveData.getValue() == null && Objects.equals(_currentMonthLiveData.getValue(), monthToRefresh)) {
            logger.debug(TAG, "refreshDataForMonth: " + monthToRefresh);
            _currentMonthLiveData.postValue(YearMonth.from(monthToRefresh));
        }
    }

    private List<CalendarTaskSummary> filterAndSortTasks(
            @Nullable List<CalendarTaskSummary> tasks, @Nullable LocalDate selectedDate,
            @NonNull Set<TaskFilterOption> filterOptions, @NonNull TaskSortOption sortOption
    ) {
        if (tasks == null) return Collections.emptyList();
        logger.debug(TAG, "filterAndSortTasks: Input " + tasks.size() + " tasks. Date: " + selectedDate + ", Filters: " + filterOptions + ", Sort: " + sortOption);
        Stream<CalendarTaskSummary> stream = tasks.stream();
        LocalDate today = dateTimeUtils.currentLocalDate();

        if (!filterOptions.contains(TaskFilterOption.ALL)) {
            stream = stream.filter(task -> {
                boolean passesPriority = true;
                if (filterOptions.contains(TaskFilterOption.CRITICAL_PRIORITY)) passesPriority = task.getPriority() == Priority.CRITICAL;
                else if (filterOptions.contains(TaskFilterOption.HIGH_PRIORITY)) passesPriority = task.getPriority() == Priority.HIGH || task.getPriority() == Priority.CRITICAL;

                boolean passesDateSpecific = true;
                if (selectedDate == null) {
                    if (filterOptions.contains(TaskFilterOption.TODAY)) passesDateSpecific = task.getDueDate().toLocalDate().isEqual(today);
                    else if (filterOptions.contains(TaskFilterOption.OVERDUE)) passesDateSpecific = task.getDueDate().toLocalDate().isBefore(today) && task.getStatus() != TaskStatus.DONE;
                }

                boolean passesCompletion = true;
                boolean hasCompleteFilter = filterOptions.contains(TaskFilterOption.COMPLETE);
                boolean hasIncompleteFilter = filterOptions.contains(TaskFilterOption.INCOMPLETE);

                if (hasCompleteFilter && !hasIncompleteFilter) passesCompletion = (task.getStatus() == TaskStatus.DONE);
                else if (!hasCompleteFilter && hasIncompleteFilter) passesCompletion = (task.getStatus() != TaskStatus.DONE);

                return passesPriority && passesDateSpecific && passesCompletion;
            });
        }
        List<CalendarTaskSummary> filteredList = stream.collect(Collectors.toList());
        switch (sortOption) {
            case TIME_ASC: filteredList.sort(Comparator.comparing(CalendarTaskSummary::getDueDate)); break;
            case TIME_DESC: filteredList.sort(Comparator.comparing(CalendarTaskSummary::getDueDate).reversed()); break;
            case CREATED_NEWEST: filteredList.sort(Comparator.comparingLong(CalendarTaskSummary::getId).reversed()); break;
            case CREATED_OLDEST: filteredList.sort(Comparator.comparingLong(CalendarTaskSummary::getId)); break;
            case PRIORITY_DESC: filteredList.sort(Comparator.comparing((CalendarTaskSummary t) -> t.getPriority().ordinal()).reversed()); break;
            case PRIORITY_ASC: filteredList.sort(Comparator.comparing((CalendarTaskSummary t) -> t.getPriority().ordinal())); break;
            case STATUS: filteredList.sort(Comparator.comparing(t -> t.getStatus() == TaskStatus.DONE)); break;
        }
        logger.debug(TAG, "filterAndSortTasks: Resulting " + filteredList.size() + " tasks.");
        return filteredList;
    }

    private static class TriggerData {
        final YearMonth month; final LocalDate date; final Long workspaceId;
        TriggerData(YearMonth m, LocalDate d, Long wsId) { month = m; date = d; workspaceId = wsId; }
        @Override public boolean equals(Object o) {
            if (this == o) return true; if (o == null || getClass() != o.getClass()) return false;
            TriggerData that = (TriggerData) o;
            return Objects.equals(month, that.month) && Objects.equals(date, that.date) && Objects.equals(workspaceId, that.workspaceId);
        }
        @Override public int hashCode() { return Objects.hash(month, date, workspaceId); }
    }
    private static class Quartet<F, S, T, U> {
        final F first; final S second; final T third; final U fourth;
        Quartet(F f, S s, T t, U u) { first = f; second = s; third = t; fourth = u; }
        @Override public boolean equals(Object o) {
            if (this == o) return true; if (o == null || getClass() != o.getClass()) return false;
            Quartet<?, ?, ?, ?> quartet = (Quartet<?, ?, ?, ?>) o;
            return Objects.equals(first, quartet.first) && Objects.equals(second, quartet.second) && Objects.equals(third, quartet.third) && Objects.equals(fourth, quartet.fourth);
        }
        @Override public int hashCode() { return Objects.hash(first, second, third, fourth); }
    }

    // Заглушка, чтобы код компилировался, замените на реальную логику или удалите
    public android.graphics.Color getPriorityColor(Priority priority) {
        if (priority == null) priority = Priority.LOW;
        int colorInt = switch (priority) {
            case CRITICAL -> android.graphics.Color.RED;
            case HIGH -> android.graphics.Color.rgb(255, 165, 0); // Orange
            case MEDIUM -> android.graphics.Color.YELLOW;
            case LOW -> android.graphics.Color.GREEN;
        };
        return android.graphics.Color.valueOf(colorInt);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        logger.info(TAG, "ViewModel cleared. Instance: " + this.hashCode());
    }
}