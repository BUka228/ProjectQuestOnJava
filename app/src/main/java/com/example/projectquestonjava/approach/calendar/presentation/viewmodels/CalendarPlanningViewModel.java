package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
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
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.core.domain.usecases.DeleteTaskUseCase;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;


@HiltViewModel
public class CalendarPlanningViewModel extends ViewModel {

    private static final String TAG = "CalendarPlanningVM";

    private final UpdateCalendarTaskUseCase updateTaskUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final SnackbarManager snackbarManager;
    private final Logger logger;

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

    // LiveData для CalendarMonthData, который будет результатом switchMap
    private final LiveData<CalendarMonthData> calendarDataLiveData;

    // Отфильтрованные и отсортированные задачи
    public final LiveData<List<CalendarTaskSummary>> filteredTasksLiveData;


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
            Logger logger) {
        // Нужен для getTasksForDay
        this.updateTaskUseCase = updateTaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.snackbarManager = snackbarManager;
        this.logger = logger;

        // Объединяем источники для calendarDataLiveData
        MediatorLiveData<Triple<YearMonth, LocalDate, Long>> triggerLiveData = new MediatorLiveData<>();
        triggerLiveData.addSource(_currentMonthLiveData, month -> triggerLiveData.setValue(new Triple<>(month, _selectedDateLiveData.getValue(), workspaceSessionManager.getWorkspaceIdLiveData().getValue())));
        triggerLiveData.addSource(_selectedDateLiveData, date -> triggerLiveData.setValue(new Triple<>(_currentMonthLiveData.getValue(), date, workspaceSessionManager.getWorkspaceIdLiveData().getValue())));
        triggerLiveData.addSource(workspaceSessionManager.getWorkspaceIdLiveData(), wsId -> triggerLiveData.setValue(new Triple<>(_currentMonthLiveData.getValue(), _selectedDateLiveData.getValue(), wsId)));
        // Инициализация начальным значением
        triggerLiveData.setValue(new Triple<>(_currentMonthLiveData.getValue(), _selectedDateLiveData.getValue(), workspaceSessionManager.getWorkspaceIdLiveData().getValue()));


        calendarDataLiveData = Transformations.switchMap(triggerLiveData, trigger -> {
            YearMonth month = trigger.first;
            LocalDate date = trigger.second;
            Long workspaceId = trigger.third;

            if (workspaceId == null || workspaceId == 0L) {
                logger.warn(TAG, "No active workspace, returning empty CalendarMonthData LiveData.");
                MutableLiveData<CalendarMonthData> emptyData = new MutableLiveData<>();
                emptyData.setValue(CalendarMonthData.EMPTY);
                return emptyData;
            }
            updateUiState(s -> s.copyWithLoading(true));

            if (date != null) { // Загрузка для выбранного дня
                logger.debug(TAG, "Fetching data LiveData for selected date: " + date + ", workspace: " + workspaceId);
                LiveData<List<CalendarTaskWithTagsAndPomodoro>> tasksForDay = calendarRepository.getTasksForDay(workspaceId, date.atStartOfDay());
                return Transformations.map(tasksForDay, tasksWithDetails -> {
                    updateUiState(s -> s.copyWithLoading(false));
                    if (tasksWithDetails == null) return CalendarMonthData.EMPTY;
                    List<CalendarTaskSummary> summaries = CalendarExtensions.toTaskSummaries(tasksWithDetails, priorityResolver, dateTimeUtils);
                    Map<LocalDate, Integer> counts = Collections.singletonMap(date, summaries.size());
                    return new CalendarMonthData(summaries, counts);
                });
            } else { // Загрузка для месяца
                logger.debug(TAG, "Fetching data LiveData for month: " + month + ", workspace: " + workspaceId);
                // getCalendarMonthDataUseCase.execute() уже возвращает LiveData<CalendarMonthData>
                LiveData<CalendarMonthData> monthData = getCalendarMonthDataUseCase.execute(month.atDay(1));
                // Просто добавляем обработку isLoading
                MediatorLiveData<CalendarMonthData> mediatedMonthData = new MediatorLiveData<>();
                mediatedMonthData.addSource(monthData, data -> {
                    updateUiState(s -> s.copyWithLoading(false));
                    mediatedMonthData.setValue(data != null ? data : CalendarMonthData.EMPTY);
                });
                return mediatedMonthData;
            }
        });

        // LiveData для dailyTaskCounts
        // _dailyTaskCountsLiveData теперь не нужен отдельно, он часть calendarDataLiveData

        // filteredTasksLiveData
        MediatorLiveData<Quartet<List<CalendarTaskSummary>, Set<TaskFilterOption>, TaskSortOption, LocalDate>> filterSortTrigger = new MediatorLiveData<>();
        filterSortTrigger.addSource(Transformations.map(calendarDataLiveData, CalendarMonthData::getTasks), tasks -> updateFilterSortTrigger(filterSortTrigger));
        filterSortTrigger.addSource(_filterOptionsLiveData, filters -> updateFilterSortTrigger(filterSortTrigger));
        filterSortTrigger.addSource(_sortOptionLiveData, sort -> updateFilterSortTrigger(filterSortTrigger));
        filterSortTrigger.addSource(_selectedDateLiveData, date -> updateFilterSortTrigger(filterSortTrigger));
        // Инициализация
        updateFilterSortTrigger(filterSortTrigger);


        filteredTasksLiveData = Transformations.map(filterSortTrigger, trigger -> {
            if (trigger == null) return Collections.emptyList();
            return filterAndSortTasks(trigger.first, trigger.fourth, trigger.second, trigger.third);
        });
    }

    private void updateFilterSortTrigger(MediatorLiveData<Quartet<List<CalendarTaskSummary>, Set<TaskFilterOption>, TaskSortOption, LocalDate>> mediator) {
        List<CalendarTaskSummary> tasks = calendarDataLiveData.getValue() != null ? calendarDataLiveData.getValue().getTasks() : Collections.emptyList();
        Set<TaskFilterOption> filters = _filterOptionsLiveData.getValue();
        TaskSortOption sort = _sortOptionLiveData.getValue();
        LocalDate date = _selectedDateLiveData.getValue();
        if (filters != null && sort != null) { // date может быть null
            mediator.setValue(new Quartet<>(tasks, filters, sort, date));
        }
    }

    private void updateUiState(UiStateUpdaterPlanning updater) {
        PlanningUiState currentState = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.update(currentState != null ? currentState : new PlanningUiState()));
    }

    @FunctionalInterface
    interface UiStateUpdaterPlanning {
        PlanningUiState update(PlanningUiState currentState);
    }

    // --- Методы управления UI ---
    public void updateMonth(YearMonth month) {
        if (!Objects.equals(_currentMonthLiveData.getValue(), month)) {
            logger.debug(TAG, "Updating current month to: " + month);
            _currentMonthLiveData.setValue(month);
            _selectedDateLiveData.setValue(null); // Сброс даты
        }
    }

    public void selectDate(LocalDate date) {
        LocalDate newSelectedDate = Objects.equals(_selectedDateLiveData.getValue(), date) ? null : date;
        logger.debug(TAG, "Updating selected date to: " + newSelectedDate);
        _selectedDateLiveData.setValue(newSelectedDate);
    }

    public void toggleCalendarExpanded() {
        _calendarExpandedLiveData.setValue(!Objects.requireNonNull(_calendarExpandedLiveData.getValue()));
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
        } else if (currentOptions.contains(TaskFilterOption.ALL)) {
            newOptions = Collections.singleton(option);
        } else if (currentOptions.contains(option)) {
            currentOptions.remove(option);
            newOptions = currentOptions.isEmpty() ? Collections.singleton(TaskFilterOption.INCOMPLETE) : currentOptions;
        } else {
            currentOptions.remove(TaskFilterOption.ALL); // Если был ALL, убираем его
            currentOptions.add(option);
            newOptions = currentOptions;
        }
        logger.debug(TAG, "Updating filter options to: " + newOptions);
        _filterOptionsLiveData.setValue(newOptions);
    }


    public void resetSortAndFilters() {
        logger.debug(TAG, "Resetting sort and filters to default");
        _sortOptionLiveData.setValue(TaskSortOption.TIME_ASC);
        _filterOptionsLiveData.setValue(Collections.singleton(TaskFilterOption.INCOMPLETE));
    }
    public void clearError() { updateUiState(s -> s.copy(null,null, null)); }
    public void clearSuccessMessage() { updateUiState(s -> s.copy(null,null, null)); }


    public void deleteTask(long taskId) {
        logger.debug(TAG, "Attempting to delete task: taskId=" + taskId);
        updateUiState(s -> s.copyWithLoading(true));
        ListenableFuture<Void> future = deleteTaskUseCase.execute(taskId);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Task " + taskId + " deleted successfully.");
                updateUiState(s -> s.copyWithLoading(false));
                snackbarManager.showMessage("Задача удалена");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Error deleting task " + taskId, t);
                updateUiState(s -> s.copy(false, "Ошибка удаления: " + t.getMessage(), null));
                snackbarManager.showMessage("Ошибка удаления: " + t.getMessage());
            }
        }, MoreExecutors.directExecutor());
    }

    public void requestMoveTask(long taskId) {
        logger.debug(TAG, "Requesting move for task " + taskId);
        _taskToMoveIdLiveData.setValue(taskId);
        _showMoveTaskSheetLiveData.setValue(true);
    }

    public void onMoveDateSelected(LocalDate newDate) {
        _showMoveTaskSheetLiveData.setValue(false);
        Long taskId = _taskToMoveIdLiveData.getValue();
        if (taskId == null) {
            onMoveCancelled();
            return;
        }
        logger.debug(TAG, "Attempting to move task " + taskId + " to " + newDate);
        updateUiState(s ->  s.copyWithLoading(true));

        // Получаем текущую задачу (может быть сложно без блокировки или изменения filteredTasksLiveData на ListenableFuture)
        // Для простоты, предположим, что мы можем найти ее в текущем значении LiveData
        List<CalendarTaskSummary> currentTasks = filteredTasksLiveData.getValue();
        CalendarTaskSummary originalTaskSummary = null;
        if (currentTasks != null) {
            for (CalendarTaskSummary summary : currentTasks) {
                if (summary.getId() == taskId) {
                    originalTaskSummary = summary;
                    break;
                }
            }
        }

        if (originalTaskSummary == null) {
            logger.error(TAG, "Task " + taskId + " not found for move.");
            updateUiState(s -> s.copy(false, "Задача не найдена", null));
            onMoveCancelled();
            return;
        }

        LocalDateTime originalTime = LocalDateTime.from(originalTaskSummary.getDueDate().toLocalTime());
        LocalDateTime newDateTime = LocalDateTime.of(newDate, LocalTime.from(originalTime));
        TaskInput taskInput = new TaskInput(
                originalTaskSummary.getId(),
                originalTaskSummary.getTitle(),
                originalTaskSummary.getDescription(),
                newDateTime, // Локальное время
                originalTaskSummary.getRecurrenceRule(),
                new HashSet<>(originalTaskSummary.getTags())
        );

        ListenableFuture<Void> updateFuture = updateTaskUseCase.execute(taskInput);
        final CalendarTaskSummary finalOriginalTaskSummary = originalTaskSummary; // для лямбды
        Futures.addCallback(updateFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Task " + finalOriginalTaskSummary.getId() + " successfully moved to " + newDate + ".");
                updateUiState(s -> s.copyWithLoading(false));
                snackbarManager.showMessage("Задача перемещена на " + newDate.format(DateTimeFormatter.ofPattern("d MMMM")));
                // Логика обновления месяца
                YearMonth newMonth = YearMonth.from(newDate);
                if (!Objects.equals(newMonth, _currentMonthLiveData.getValue()) && _selectedDateLiveData.getValue() == null) {
                    updateMonth(newMonth); // Вызываем синхронно, т.к. это обновление LiveData
                }
                onMoveCancelled(); // Сбрасываем taskToMoveId
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Error moving task " + finalOriginalTaskSummary.getId(), t);
                updateUiState(s -> s.copy(false, "Ошибка перемещения: " + t.getMessage(), null));
                onMoveCancelled();
            }
        }, MoreExecutors.directExecutor());
    }


    public void onMoveCancelled() {
        logger.debug(TAG, "Move cancelled");
        _taskToMoveIdLiveData.setValue(null);
        _showMoveTaskSheetLiveData.setValue(false);
    }

    private List<CalendarTaskSummary> filterAndSortTasks(
            List<CalendarTaskSummary> tasks,
            LocalDate selectedDate,
            Set<TaskFilterOption> filterOptions,
            TaskSortOption sortOption
    ) {
        if (tasks == null) return Collections.emptyList();
        Stream<CalendarTaskSummary> stream = tasks.stream();
        LocalDate today = LocalDate.now();

        if (!filterOptions.contains(TaskFilterOption.ALL)) {
            stream = stream.filter(task -> {
                boolean passesPriority = true;
                if (filterOptions.contains(TaskFilterOption.CRITICAL_PRIORITY)) passesPriority = task.getPriority() == Priority.CRITICAL;
                else if (filterOptions.contains(TaskFilterOption.HIGH_PRIORITY)) passesPriority = task.getPriority() == Priority.HIGH || task.getPriority() == Priority.CRITICAL;

                boolean passesDateSpecific = true;
                if (selectedDate == null) { // Только если НЕ выбран конкретный день
                    if (filterOptions.contains(TaskFilterOption.TODAY)) passesDateSpecific = task.getDueDate().toLocalDate().isEqual(today);
                    else if (filterOptions.contains(TaskFilterOption.OVERDUE)) passesDateSpecific = task.getDueDate().toLocalDate().isBefore(today) && task.getStatus() != TaskStatus.DONE;
                }

                boolean passesCompletion = true;
                // Если выбраны оба (COMPLETE и INCOMPLETE) или ни один из них (кроме ALL), то пропускаем все по статусу
                boolean onlyComplete = filterOptions.contains(TaskFilterOption.COMPLETE);
                boolean onlyIncomplete = filterOptions.contains(TaskFilterOption.INCOMPLETE);

                if (onlyComplete && !onlyIncomplete) passesCompletion = task.getStatus() == TaskStatus.DONE;
                else if (!onlyComplete && onlyIncomplete) passesCompletion = task.getStatus() != TaskStatus.DONE;
                // Если выбраны оба или ни один, passesCompletion остается true

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
        return filteredList;
    }

    // Вспомогательные классы для MediatorLiveData
    private static class Triple<F, S, T> {
        final F first;
        final S second;
        final T third;
        Triple(F f, S s, T t) { first = f; second = s; third = t; }
    }
    private static class Quartet<F, S, T, U> {
        final F first;
        final S second;
        final T third;
        final U fourth;
        Quartet(F f, S s, T t, U u) { first = f; second = s; third = t; fourth = u; }
    }

    public android.graphics.Color getPriorityColor(Priority priority) {
        // Заглушка, т.к. ViewModel не должна знать о цветах Compose
        // В реальном приложении это будет решаться в UI на основе данных из ViewModel
        return switch (priority) {
            case CRITICAL -> new android.graphics.Color(); // android.graphics.Color.RED;
            case HIGH ->
                    new android.graphics.Color(); // android.graphics.Color.rgb(255, 165, 0); // Orange
            case MEDIUM -> new android.graphics.Color(); // android.graphics.Color.YELLOW;
            case LOW -> new android.graphics.Color(); // android.graphics.Color.GREEN;

        };
    }

}