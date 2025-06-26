package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.di.MainExecutor; // Добавляем импорт
import com.example.projectquestonjava.core.domain.usecases.DeleteTaskUseCase;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.SingleLiveEvent;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarDashboardData;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskFilterOption;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskSortOption;
import com.example.projectquestonjava.approach.calendar.domain.usecases.GetDashboardDataUseCase;
import com.example.projectquestonjava.approach.calendar.domain.usecases.MarkTaskAsIncompleteUseCase;
import com.example.projectquestonjava.feature.gamification.domain.usecases.ProcessTaskCompletionUseCase;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

@HiltViewModel
public class CalendarDashboardViewModel extends ViewModel {

    public enum SwipeDirection { LEFT, RIGHT }

    public static abstract class SwipeActionState {
        private SwipeActionState() {}
        public static final class Idle extends SwipeActionState {
            private static final Idle INSTANCE = new Idle();
            public static Idle getInstance() { return INSTANCE; }
        }
        public static final class ConfirmingDelete extends SwipeActionState {
            private final CalendarTaskSummary taskSummary;
            public ConfirmingDelete(CalendarTaskSummary taskSummary) { this.taskSummary = taskSummary; }
            public CalendarTaskSummary getTaskSummary() { return taskSummary; }
        }
    }

    public static final int DATE_RANGE = 30;
    public static final int INITIAL_PAGE = DATE_RANGE;
    private static final String TAG = "CalendarDashboardVM";

    private final GetDashboardDataUseCase getDashboardDataUseCase;
    private final ProcessTaskCompletionUseCase processTaskCompletionUseCase;
    private final MarkTaskAsIncompleteUseCase markTaskAsIncompleteUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final DateTimeUtils dateTimeUtils;
    private final SnackbarManager snackbarManager;
    private final Logger logger;
    private final Executor ioExecutor;
    private final Executor mainExecutor;

    private final MutableLiveData<LocalDateTime> _selectedDateLiveData;
    public final LiveData<LocalDateTime> selectedDateLiveData;

    private final MutableLiveData<Integer> _currentPageLiveData;
    public final LiveData<Integer> currentPageLiveData;

    private final MutableLiveData<TaskSortOption> _sortOptionLiveData;
    public final LiveData<TaskSortOption> sortOptionLiveData;

    private final MutableLiveData<Set<TaskFilterOption>> _filterOptionsLiveData;
    public final LiveData<Set<TaskFilterOption>> filterOptionsLiveData;

    private final MutableLiveData<Set<Tag>> _selectedTagsLiveData;
    public final LiveData<Set<Tag>> selectedTagsLiveData;

    private final MutableLiveData<Boolean> _showCalendarDialogLiveData;
    public final LiveData<Boolean> showCalendarDialogLiveData;

    private final MutableLiveData<SwipeActionState> _swipeActionStateLiveData;
    public final LiveData<SwipeActionState> swipeActionStateLiveData;

    private final LiveData<CalendarDashboardData> _rawDataForSelectedDate;
    public final LiveData<CalendarDashboardData> dashboardDataLiveData;
    public final LiveData<Map<LocalDate, CalendarDashboardData>> pagerDataLiveData;

    public final LiveData<Float> currentProgressLiveData;

    private final List<LocalDateTime> pageDates;

    private final MutableLiveData<Long> _requestedTaskIdForDetails;
    public final LiveData<CalendarTaskSummary> taskDetailsForBottomSheetLiveData;

    private final SingleLiveEvent<Long> _navigateToEditTaskEvent;
    public LiveData<Long> navigateToEditTaskEvent;
    private final SingleLiveEvent<Long> _navigateToPomodoroEvent;
    public LiveData<Long> navigateToPomodoroEvent;

    private final MediatorLiveData<CombinedDataTrigger> dataProcessingTrigger = new MediatorLiveData<>();

    @Inject
    public CalendarDashboardViewModel(
            GetDashboardDataUseCase getDashboardDataUseCase,
            ProcessTaskCompletionUseCase processTaskCompletionUseCase,
            MarkTaskAsIncompleteUseCase markTaskAsIncompleteUseCase,
            DeleteTaskUseCase deleteTaskUseCase,
            DateTimeUtils dateTimeUtils,
            SnackbarManager snackbarManager,
            Logger logger,
            @IODispatcher Executor ioExecutor,
            @MainExecutor Executor mainExecutor) {

        this.getDashboardDataUseCase = getDashboardDataUseCase;
        this.processTaskCompletionUseCase = processTaskCompletionUseCase;
        this.markTaskAsIncompleteUseCase = markTaskAsIncompleteUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.dateTimeUtils = dateTimeUtils;
        this.snackbarManager = snackbarManager;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutor; // Сохраняем MainExecutor

        logger.info(TAG, "ViewModel initialized. Instance: " + this.hashCode());

        _selectedDateLiveData = new MutableLiveData<>(dateTimeUtils.currentLocalDateTime().with(LocalTime.NOON));
        selectedDateLiveData = _selectedDateLiveData;
        _currentPageLiveData = new MutableLiveData<>(INITIAL_PAGE);
        currentPageLiveData = _currentPageLiveData;

        _sortOptionLiveData = new MutableLiveData<>(TaskSortOption.TIME_ASC);
        sortOptionLiveData = _sortOptionLiveData;
        _filterOptionsLiveData = new MutableLiveData<>(Collections.singleton(TaskFilterOption.ALL));
        filterOptionsLiveData = _filterOptionsLiveData;
        _selectedTagsLiveData = new MutableLiveData<>(Collections.emptySet());
        selectedTagsLiveData = _selectedTagsLiveData;

        _showCalendarDialogLiveData = new MutableLiveData<>(false);
        showCalendarDialogLiveData = _showCalendarDialogLiveData;
        _swipeActionStateLiveData = new MutableLiveData<>(SwipeActionState.Idle.getInstance());
        swipeActionStateLiveData = _swipeActionStateLiveData;

        pageDates = IntStream.range(0, DATE_RANGE * 2 + 1)
                .mapToObj(offset -> dateTimeUtils.currentLocalDateTime().plusDays((long) offset - INITIAL_PAGE).with(LocalTime.NOON))
                .collect(Collectors.toList());

        _rawDataForSelectedDate = Transformations.switchMap(_selectedDateLiveData, date -> {
            logger.debug(TAG, "_rawDataForSelectedDate triggered by selectedDate change: " + date.toLocalDate());
            return getDashboardDataUseCase.execute(date);
        });

        dataProcessingTrigger.addSource(_rawDataForSelectedDate, rawData -> {
            logger.debug(TAG, "dataProcessingTrigger: _rawDataForSelectedDate emitted " + (rawData != null && rawData.getTasks() != null ? rawData.getTasks().size() : "null/0") + " tasks.");
            dataProcessingTrigger.setValue(buildDataTrigger(rawData));
        });
        dataProcessingTrigger.addSource(_selectedTagsLiveData, tags -> dataProcessingTrigger.setValue(buildDataTrigger(_rawDataForSelectedDate.getValue())));
        dataProcessingTrigger.addSource(_sortOptionLiveData, sort -> dataProcessingTrigger.setValue(buildDataTrigger(_rawDataForSelectedDate.getValue())));
        dataProcessingTrigger.addSource(_filterOptionsLiveData, filters -> dataProcessingTrigger.setValue(buildDataTrigger(_rawDataForSelectedDate.getValue())));

        dashboardDataLiveData = Transformations.map(dataProcessingTrigger, trigger -> {
            LocalDateTime currentSelectedDateVal = _selectedDateLiveData.getValue();
            String dateLog = (currentSelectedDateVal != null) ? currentSelectedDateVal.toLocalDate().toString() : "null_date";
            if (trigger == null || trigger.rawData == null) {
                logger.warn(TAG, "dashboardDataLiveData: Trigger or rawData is null for " + dateLog + ". Returning EMPTY.");
                return CalendarDashboardData.EMPTY;
            }
            logger.debug(TAG, "dashboardDataLiveData: Applying filters/sort to raw data for " + dateLog);
            return applyFiltersAndSorting(trigger.rawData, trigger.tags, trigger.sortOption, trigger.filterOptions);
        });

        pagerDataLiveData = Transformations.switchMap(dataProcessingTrigger, trigger -> {
            MediatorLiveData<Map<LocalDate, CalendarDashboardData>> pageDataMediator = new MediatorLiveData<>();
            Integer currentPage = _currentPageLiveData.getValue();
            if (currentPage == null || trigger == null) {
                pageDataMediator.setValue(Collections.emptyMap());
                return pageDataMediator;
            }
            List<LocalDate> datesInRange = getDatesForPagerRange(currentPage);
            Map<LocalDate, CalendarDashboardData> rangeDataMap = new HashMap<>();
            AtomicInteger loadedDataCount = new AtomicInteger(0);
            int totalDatesToLoad = datesInRange.size();

            for (LocalDate dateKey : datesInRange) {
                LiveData<CalendarDashboardData> rawDataForDateKey = getDashboardDataUseCase.execute(dateKey.atTime(LocalTime.NOON));
                pageDataMediator.addSource(rawDataForDateKey, rawData -> {
                    if (rawData != null) {
                        CalendarDashboardData filteredData = applyFiltersAndSorting(rawData, trigger.tags, trigger.sortOption, trigger.filterOptions);
                        rangeDataMap.put(dateKey, filteredData);
                    } else {
                        rangeDataMap.put(dateKey, CalendarDashboardData.EMPTY);
                    }
                    if (rangeDataMap.size() == totalDatesToLoad) {
                        logger.debug(TAG, "pagerDataLiveData: All data for range around page " + currentPage + " processed. Emitting map with " + rangeDataMap.size() + " entries.");
                        pageDataMediator.setValue(new HashMap<>(rangeDataMap));
                    }
                });
            }
            return pageDataMediator;
        });

        currentProgressLiveData = Transformations.map(dashboardDataLiveData, data -> {
            if (data == null || data.getTasks() == null || data.getTasks().isEmpty()) return 0f;
            List<CalendarTaskSummary> tasks = data.getTasks();
            long completedCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            return tasks.isEmpty() ? 0f : (float) completedCount / tasks.size();
        });

        _requestedTaskIdForDetails = new MutableLiveData<>(null);
        taskDetailsForBottomSheetLiveData = Transformations.switchMap(_requestedTaskIdForDetails, taskId -> {
            if (taskId == null || taskId == -1L) return new MutableLiveData<>(null);
            return Transformations.map(dashboardDataLiveData, currentDayData -> {
                if (currentDayData == null || currentDayData.getTasks() == null) return null;
                return currentDayData.getTasks().stream().filter(task -> task.getId() == taskId).findFirst().orElse(null);
            });
        });

        _navigateToEditTaskEvent = new SingleLiveEvent<>();
        navigateToEditTaskEvent = _navigateToEditTaskEvent;
        _navigateToPomodoroEvent = new SingleLiveEvent<>();
        navigateToPomodoroEvent = _navigateToPomodoroEvent;
    }

    private CombinedDataTrigger buildDataTrigger(CalendarDashboardData rawData) {
        Set<Tag> tags = _selectedTagsLiveData.getValue();
        TaskSortOption sort = _sortOptionLiveData.getValue();
        Set<TaskFilterOption> filters = _filterOptionsLiveData.getValue();
        if (tags == null) tags = Collections.emptySet();
        if (sort == null) sort = TaskSortOption.TIME_ASC;
        if (filters == null) filters = Collections.singleton(TaskFilterOption.ALL);
        return new CombinedDataTrigger(rawData, tags, sort, filters);
    }

    private List<LocalDate> getDatesForPagerRange(int currentPage) {
        List<LocalDate> dates = new ArrayList<>();
        int prefetch = 1;
        LocalDate centerDate = getDateForPageInternal(currentPage).toLocalDate();
        for (int i = -prefetch; i <= prefetch; i++) {
            dates.add(centerDate.plusDays(i));
        }
        return dates.stream().distinct().sorted().collect(Collectors.toList());
    }

    public void refreshDataForCurrentPage() {
        logger.info(TAG, "refreshDataForCurrentPage called.");
        LocalDateTime currentDate = _selectedDateLiveData.getValue();
        if (currentDate != null) {
            // Используем postValue, если есть вероятность вызова не из MainThread,
            // но так как этот метод вызывается из коллбэка Future, который может быть на ioExecutor,
            // лучше переключить обновление LiveData на главный поток.
            final LocalDateTime newDateObject = LocalDateTime.of(currentDate.toLocalDate(), currentDate.toLocalTime());
            mainExecutor.execute(() -> _selectedDateLiveData.setValue(newDateObject));
        }
    }

    private CalendarDashboardData applyFiltersAndSorting(
            CalendarDashboardData data, Set<Tag> selectedTags,
            TaskSortOption sortOption, Set<TaskFilterOption> filterOptions) {
        if (data == null || data.getTasks() == null) {
            logger.debug(TAG, "applyFiltersAndSorting: Input data or tasks are null. Returning EMPTY.");
            return CalendarDashboardData.EMPTY;
        }
        List<CalendarTaskSummary> tasksToProcess = new ArrayList<>(data.getTasks());
        logger.debug(TAG, "applyFiltersAndSorting: Input tasks: " + tasksToProcess.size() +
                ", Tags: " + (selectedTags != null ? selectedTags.size() : "0") +
                ", Sort: " + sortOption +
                ", Filters: " + filterOptions);

        final boolean hasTagFilter = selectedTags != null && !selectedTags.isEmpty();
        final boolean isAllFilterSelected = filterOptions.contains(TaskFilterOption.ALL);

        List<CalendarTaskSummary> filteredTasks = tasksToProcess.stream()
                .filter(task -> {
                    if (hasTagFilter) {
                        if (task.getTags() == null || task.getTags().stream().noneMatch(selectedTags::contains)) {
                            return false;
                        }
                    }
                    if (isAllFilterSelected) return true;

                    boolean passesStatus = true;
                    boolean hasSpecificStatusFilter = false;
                    if (filterOptions.contains(TaskFilterOption.COMPLETE)) {
                        passesStatus = (task.getStatus() == TaskStatus.DONE);
                        hasSpecificStatusFilter = true;
                    }
                    if (filterOptions.contains(TaskFilterOption.INCOMPLETE)) {
                        if (hasSpecificStatusFilter && passesStatus) { passesStatus = false; }
                        else if (!hasSpecificStatusFilter) { passesStatus = (task.getStatus() != TaskStatus.DONE); }
                        hasSpecificStatusFilter = true;
                    }
                    if (!hasSpecificStatusFilter) passesStatus = true;

                    boolean passesPriority = true;
                    if (filterOptions.contains(TaskFilterOption.CRITICAL_PRIORITY) && task.getPriority() != Priority.CRITICAL) passesPriority = false;
                    if (filterOptions.contains(TaskFilterOption.HIGH_PRIORITY) && task.getPriority() != Priority.HIGH && task.getPriority() != Priority.CRITICAL) passesPriority = false;

                    return passesStatus && passesPriority;
                })
                .collect(Collectors.toList());
        logger.debug(TAG, "applyFiltersAndSorting: Tasks after filtering: " + filteredTasks.size());

        switch (sortOption) {
            case TIME_ASC: filteredTasks.sort(Comparator.comparing(CalendarTaskSummary::getDueDate)); break;
            case TIME_DESC: filteredTasks.sort(Comparator.comparing(CalendarTaskSummary::getDueDate).reversed()); break;
            case CREATED_NEWEST: filteredTasks.sort(Comparator.comparingLong(CalendarTaskSummary::getId).reversed()); break;
            case CREATED_OLDEST: filteredTasks.sort(Comparator.comparingLong(CalendarTaskSummary::getId)); break;
            case PRIORITY_DESC: filteredTasks.sort(Comparator.comparing((CalendarTaskSummary t) -> t.getPriority().ordinal()).reversed()); break;
            case PRIORITY_ASC: filteredTasks.sort(Comparator.comparing((CalendarTaskSummary t) -> t.getPriority().ordinal())); break;
            case STATUS: filteredTasks.sort(Comparator.comparing(t -> t.getStatus() == TaskStatus.DONE)); break;
        }
        logger.debug(TAG, "applyFiltersAndSorting: Tasks after sorting: " + filteredTasks.size());
        return new CalendarDashboardData(filteredTasks, data.getGamification());
    }

    public void updateSortOption(TaskSortOption option) { _sortOptionLiveData.setValue(option); }
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
            newOptions = currentOptions.isEmpty() ? Collections.singleton(TaskFilterOption.ALL) : currentOptions;
        }
        _filterOptionsLiveData.setValue(newOptions);
    }
    public void resetSortAndFilters() {
        _sortOptionLiveData.setValue(TaskSortOption.TIME_ASC);
        _filterOptionsLiveData.setValue(Collections.singleton(TaskFilterOption.ALL));
        _selectedTagsLiveData.setValue(Collections.emptySet());
    }
    public void addTagToFilter(Tag tag) {
        Set<Tag> currentTags = new HashSet<>(Objects.requireNonNull(_selectedTagsLiveData.getValue()));
        currentTags.add(tag);
        _selectedTagsLiveData.setValue(currentTags);
    }
    public void removeTagFromFilter(Tag tag) {
        Set<Tag> currentTags = new HashSet<>(Objects.requireNonNull(_selectedTagsLiveData.getValue()));
        currentTags.remove(tag);
        _selectedTagsLiveData.setValue(currentTags);
    }
    public void clearTagFilters() { _selectedTagsLiveData.setValue(Collections.emptySet()); }

    public void onPageChanged(int page) {
        if (Objects.equals(_currentPageLiveData.getValue(), page)) return;
        if (page >= 0 && page < pageDates.size()) {
            _currentPageLiveData.postValue(page); // Используем postValue, т.к. может быть вызван из ViewPager callback
            _selectedDateLiveData.postValue(getDateForPageInternal(page));
            logger.debug(TAG, "onPageChanged: Page set to " + page + ", SelectedDate: " + getDateForPageInternal(page).toLocalDate());
        } else {
            logger.warn(TAG, "Attempted to change to invalid page index: " + page);
        }
    }

    public void selectDate(LocalDateTime newDate) {
        LocalDateTime normalizedNewDate = newDate.with(LocalTime.NOON);
        LocalDate today = dateTimeUtils.currentLocalDate();
        LocalDate targetDate = normalizedNewDate.toLocalDate();
        long daysDifference = ChronoUnit.DAYS.between(today, targetDate);
        int targetPage = INITIAL_PAGE + (int)daysDifference;

        if (targetPage >= 0 && targetPage < pageDates.size()) {
            logger.info(TAG, "selectDate: New date selected " + normalizedNewDate.toLocalDate() + ", targetPage " + targetPage);
            _selectedDateLiveData.setValue(normalizedNewDate);
            _currentPageLiveData.setValue(targetPage);
        } else {
            logger.warn(TAG, "Selected date " + targetDate + " is outside the pager range (targetPage=" + targetPage + ").");
            snackbarManager.showMessage("Выбранная дата вне доступного диапазона.");
        }
    }

    private LocalDateTime getDateForPageInternal(int page) {
        if (page >= 0 && page < pageDates.size()) {
            return pageDates.get(page);
        }
        return dateTimeUtils.currentLocalDateTime().with(LocalTime.NOON);
    }
    public LocalDateTime getDateForPage(int page) { return getDateForPageInternal(page); }

    public void showCalendarDialog() { _showCalendarDialogLiveData.setValue(true); }
    public void hideCalendarDialog() { _showCalendarDialogLiveData.setValue(false); }

    public void updateTaskStatus(long taskId, List<Tag> tags, boolean isDone) {
        logger.debug(TAG, "Updating task status: taskId=" + taskId + ", isDone=" + isDone);
        ListenableFuture<Void> future = isDone ?
                processTaskCompletionUseCase.execute(taskId, tags) :
                markTaskAsIncompleteUseCase.execute(taskId);

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {
                // ЭТОТ ЛОГ КРИТИЧЕСКИ ВАЖЕН
                logger.info(TAG, "ViewModel: Task status update UC successful for taskId=" + taskId + ". Triggering refresh.");
                refreshDataForCurrentPage();
            }
            @Override public void onFailure(@NonNull Throwable t) {
                // И ЭТОТ ЛОГ
                logger.error(TAG, "ViewModel: Task status update UC FAILED for " + taskId, t);
                mainExecutor.execute(() -> snackbarManager.showMessage("Ошибка обновления статуса: " + t.getMessage()));
            }
        }, ioExecutor); // ioExecutor, так как refreshDataForCurrentPage пинает LiveData
    }

    private void requestDeleteConfirmation(CalendarTaskSummary taskSummary) {
        _swipeActionStateLiveData.setValue(new SwipeActionState.ConfirmingDelete(taskSummary));
    }

    public void confirmDeleteTask(long taskId) {
        logger.debug(TAG, "Deleting task confirmed: taskId=" + taskId);
        _swipeActionStateLiveData.postValue(SwipeActionState.Idle.getInstance()); // Используем postValue
        ListenableFuture<Void> future = deleteTaskUseCase.execute(taskId);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {
                mainExecutor.execute(() -> snackbarManager.showMessage("Задача удалена")); // Обновление UI на главном потоке
                refreshDataForCurrentPage();
            }
            @Override public void onFailure(@NonNull Throwable t) {
                mainExecutor.execute(() -> snackbarManager.showMessage("Ошибка удаления: " + t.getMessage()));
                logger.error(TAG, "Failed to delete task " + taskId, t);
            }
        }, ioExecutor);
    }

    public void clearSwipeActionState() {
        if (!(_swipeActionStateLiveData.getValue() instanceof SwipeActionState.Idle)) {
            _swipeActionStateLiveData.postValue(SwipeActionState.Idle.getInstance());
        }
    }
    public void handleSwipeAction(long taskId, SwipeDirection direction) {
        LocalDateTime currentSelectedDateTime = _selectedDateLiveData.getValue();
        if (currentSelectedDateTime == null) { logger.error(TAG, "selectedDate is null in handleSwipeAction"); return; }
        CalendarDashboardData dataForSelectedDay = dashboardDataLiveData.getValue();

        if (dataForSelectedDay == null || dataForSelectedDay.getTasks() == null) {
            logger.error(TAG, "Cannot handle swipe for task " + taskId + ": data for selected day are null.");
            return;
        }
        CalendarTaskSummary taskToActOn = dataForSelectedDay.getTasks().stream()
                .filter(t -> t.getId() == taskId).findFirst().orElse(null);
        if (taskToActOn == null) {
            logger.error(TAG, "Cannot handle swipe: task " + taskId + " not found in current day's filtered data.");
            return;
        }
        if (direction == SwipeDirection.RIGHT) {
            updateTaskStatus(taskId, taskToActOn.getTags(), taskToActOn.getStatus() != TaskStatus.DONE);
        } else if (direction == SwipeDirection.LEFT) {
            requestDeleteConfirmation(taskToActOn);
        }
    }
    public void showTaskDetailsBottomSheet(CalendarTaskSummary task) { _requestedTaskIdForDetails.setValue(task.getId());}
    public void clearRequestedTaskDetails() { if (_requestedTaskIdForDetails.getValue() != null) _requestedTaskIdForDetails.setValue(null); }
    public void startPomodoroForTask(long taskId) { _navigateToPomodoroEvent.setValue(taskId); }
    public void editTask(long taskId) { _navigateToEditTaskEvent.setValue(taskId); }
    public void clearNavigateToEditTask() { _navigateToEditTaskEvent.setValue(null); }
    public void clearNavigateToPomodoro() { _navigateToPomodoroEvent.setValue(null); }

    private static class CombinedDataTrigger {
        final CalendarDashboardData rawData;
        final Set<Tag> tags;
        final TaskSortOption sortOption;
        final Set<TaskFilterOption> filterOptions;
        CombinedDataTrigger(CalendarDashboardData rawData, Set<Tag> tags, TaskSortOption sortOption, Set<TaskFilterOption> filterOptions) {
            this.rawData = rawData; this.tags = tags; this.sortOption = sortOption; this.filterOptions = filterOptions;
        }
        @Override public boolean equals(Object o) {  return true; }
        @Override public int hashCode() { return 1;}
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        logger.info(TAG, "ViewModel cleared. Instance: " + this.hashCode());
    }
}