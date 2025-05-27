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
import com.example.projectquestonjava.core.domain.usecases.DeleteTaskUseCase;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.SingleLiveEvent;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarDashboardData;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
// import com.example.projectquestonjava.approach.calendar.domain.model.SortOptionData; // Не используется напрямую здесь
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final int PREFETCH_DISTANCE = 3;
    private static final String TAG = "CalendarDashboardVM";

    private final GetDashboardDataUseCase getDashboardDataUseCase;
    private final ProcessTaskCompletionUseCase processTaskCompletionUseCase;
    private final MarkTaskAsIncompleteUseCase markTaskAsIncompleteUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final DateTimeUtils dateTimeUtils;
    private final SnackbarManager snackbarManager;
    private final Logger logger;
    private final Executor ioExecutor;

    private final MutableLiveData<LocalDateTime> _selectedDateLiveData;
    public final LiveData<LocalDateTime> selectedDateLiveData;

    private final MutableLiveData<Integer> _currentPageLiveData;
    public final LiveData<Integer> currentPageLiveData;

    private final MutableLiveData<TaskSortOption> _sortOptionLiveData;
    public final LiveData<TaskSortOption> sortOptionLiveData;

    private final MutableLiveData<Set<TaskFilterOption>> _filterOptionsLiveData;
    public final LiveData<Set<TaskFilterOption>> filterOptionsLiveData;

    private final SingleLiveEvent<String> _snackbarMessageEvent;
    public final LiveData<String> snackbarMessageEvent;

    private final MutableLiveData<Set<Tag>> _selectedTagsLiveData;
    public final LiveData<Set<Tag>> selectedTagsLiveData;

    private final MutableLiveData<Boolean> _showCalendarDialogLiveData;
    public final LiveData<Boolean> showCalendarDialogLiveData;

    private final MutableLiveData<SwipeActionState> _swipeActionStateLiveData;
    public final LiveData<SwipeActionState> swipeActionStateLiveData;

    private final MediatorLiveData<CombinedPagerTrigger> pagerTriggerMediator;
    public final LiveData<Map<LocalDate, CalendarDashboardData>> pagerDataLiveData;
    public final LiveData<CalendarDashboardData> dashboardDataLiveData;
    public final LiveData<Float> currentProgressLiveData;

    private final List<LocalDateTime> pageDates;

    private final MutableLiveData<Long> _requestedTaskIdForDetails; // Сделаем его private
    public final LiveData<CalendarTaskSummary> taskDetailsForBottomSheetLiveData; // Публичный LiveData

    private final SingleLiveEvent<Long> _navigateToEditTaskEvent;
    public LiveData<Long> navigateToEditTaskEvent;
    private final SingleLiveEvent<Long> _navigateToPomodoroEvent;
    public LiveData<Long> navigateToPomodoroEvent;


    @Inject
    public CalendarDashboardViewModel(
            GetDashboardDataUseCase getDashboardDataUseCase,
            ProcessTaskCompletionUseCase processTaskCompletionUseCase,
            MarkTaskAsIncompleteUseCase markTaskAsIncompleteUseCase,
            DeleteTaskUseCase deleteTaskUseCase,
            DateTimeUtils dateTimeUtils,
            SnackbarManager snackbarManager,
            Logger logger,
            @IODispatcher Executor ioExecutor) {

        this.getDashboardDataUseCase = getDashboardDataUseCase;
        this.processTaskCompletionUseCase = processTaskCompletionUseCase;
        this.markTaskAsIncompleteUseCase = markTaskAsIncompleteUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.dateTimeUtils = dateTimeUtils;
        this.snackbarManager = snackbarManager;
        this.logger = logger;
        this.ioExecutor = ioExecutor;

        _selectedDateLiveData = new MutableLiveData<>(dateTimeUtils.currentLocalDateTime());
        selectedDateLiveData = _selectedDateLiveData;

        _currentPageLiveData = new MutableLiveData<>(INITIAL_PAGE);
        currentPageLiveData = _currentPageLiveData;

        _sortOptionLiveData = new MutableLiveData<>(TaskSortOption.TIME_ASC);
        sortOptionLiveData = _sortOptionLiveData;

        _filterOptionsLiveData = new MutableLiveData<>(Collections.singleton(TaskFilterOption.ALL));
        filterOptionsLiveData = _filterOptionsLiveData;

        _snackbarMessageEvent = new SingleLiveEvent<>();
        snackbarMessageEvent = _snackbarMessageEvent;

        _selectedTagsLiveData = new MutableLiveData<>(Collections.emptySet());
        selectedTagsLiveData = _selectedTagsLiveData;

        _showCalendarDialogLiveData = new MutableLiveData<>(false);
        showCalendarDialogLiveData = _showCalendarDialogLiveData;

        _swipeActionStateLiveData = new MutableLiveData<>(SwipeActionState.Idle.getInstance());
        swipeActionStateLiveData = _swipeActionStateLiveData;

        pageDates = IntStream.range(0, DATE_RANGE * 2 + 1)
                .mapToObj(offset -> dateTimeUtils.currentLocalDateTime().plusDays((long) offset - INITIAL_PAGE))
                .collect(Collectors.toList());

        pagerTriggerMediator = new MediatorLiveData<>();
        pagerTriggerMediator.addSource(_currentPageLiveData, value -> pagerTriggerMediator.setValue(buildTrigger()));
        pagerTriggerMediator.addSource(_selectedTagsLiveData, value -> pagerTriggerMediator.setValue(buildTrigger()));
        pagerTriggerMediator.addSource(_sortOptionLiveData, value -> pagerTriggerMediator.setValue(buildTrigger()));
        pagerTriggerMediator.addSource(_filterOptionsLiveData, value -> pagerTriggerMediator.setValue(buildTrigger()));
        pagerTriggerMediator.setValue(buildTrigger());

        pagerDataLiveData = Transformations.switchMap(pagerTriggerMediator, trigger -> {
            if (trigger == null) {
                MutableLiveData<Map<LocalDate, CalendarDashboardData>> emptyData = new MutableLiveData<>();
                emptyData.setValue(Collections.emptyMap());
                return emptyData;
            }
            LocalDate centerDate = getDateForPageInternal(trigger.page()).toLocalDate();
            int range = PREFETCH_DISTANCE;
            List<LocalDate> datesToObserve = new ArrayList<>();
            for (int i = -range; i <= range; i++) {
                datesToObserve.add(centerDate.plusDays(i));
            }
            logger.debug(TAG, "PagerTrigger changed. Observing data for dates: " + datesToObserve + " based on page " + trigger.page());

            MediatorLiveData<Map<LocalDate, CalendarDashboardData>> combinedLiveData = new MediatorLiveData<>();
            Map<LocalDate, CalendarDashboardData> resultMap = new ConcurrentHashMap<>();
            AtomicInteger sourcesToLoadCount = new AtomicInteger(datesToObserve.size()); // Сколько источников ожидаем

            // Инициализируем resultMap пустыми значениями, чтобы избежать NPE при первом обращении
            for (LocalDate date : datesToObserve) {
                resultMap.put(date, CalendarDashboardData.EMPTY);
            }
            combinedLiveData.setValue(new ConcurrentHashMap<>(resultMap)); // Устанавливаем начальное значение

            for (LocalDate date : datesToObserve) {
                LiveData<CalendarDashboardData> singleDateLiveData = getDashboardDataUseCase.execute(date.atStartOfDay());
                // Добавляем источник и немедленно удаляем после первого ответа, чтобы избежать многократных обновлений от одного источника.
                // Это упрощенная эмуляция flatMapLatest + combine для LiveData.
                combinedLiveData.addSource(singleDateLiveData, new Observer<CalendarDashboardData>() {
                    @Override
                    public void onChanged(CalendarDashboardData rawData) {
                        combinedLiveData.removeSource(singleDateLiveData); // Удаляем источник после первого получения данных
                        ioExecutor.execute(() -> {
                            CalendarDashboardData processedData = (rawData != null)
                                    ? applyFiltersAndSorting(rawData, trigger.tags(), trigger.sort(), trigger.filters())
                                    : CalendarDashboardData.EMPTY;
                            resultMap.put(date, processedData);

                            if (sourcesToLoadCount.decrementAndGet() == 0) { // Если все источники ответили
                                combinedLiveData.postValue(new ConcurrentHashMap<>(resultMap));
                            }
                        });
                    }
                });
            }
            return combinedLiveData;
        });

        dashboardDataLiveData = Transformations.switchMap(_selectedDateLiveData, selectedDateTime ->
                Transformations.map(pagerDataLiveData, pagerData -> {
                    if (pagerData == null || selectedDateTime == null) return CalendarDashboardData.EMPTY;
                    return pagerData.getOrDefault(selectedDateTime.toLocalDate(), CalendarDashboardData.EMPTY);
                })
        );

        currentProgressLiveData = Transformations.map(dashboardDataLiveData, data -> {
            if (data == null || data.getTasks() == null || data.getTasks().isEmpty()) return 0f;
            List<CalendarTaskSummary> tasks = data.getTasks();
            long completedCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            return tasks.isEmpty() ? 0f : (float) completedCount / tasks.size();
        });

        _requestedTaskIdForDetails = new MutableLiveData<>(null); // Инициализация
        taskDetailsForBottomSheetLiveData = Transformations.switchMap(_requestedTaskIdForDetails, taskId -> {
            if (taskId == null || taskId == -1L) {
                return new MutableLiveData<>(null);
            }
            // Ищем задачу в pagerDataLiveData для выбранной даты. Это гарантирует, что мы берем
            // уже отфильтрованную и отсортированную версию, если пользователь применил фильтры/сортировку.
            return Transformations.map(dashboardDataLiveData, currentDayData -> {
                if (currentDayData == null || currentDayData.getTasks() == null) return null;
                return currentDayData.getTasks().stream()
                        .filter(task -> task.getId() == taskId)
                        .findFirst()
                        .orElse(null);
            });
        });

        _navigateToEditTaskEvent = new SingleLiveEvent<>();
        navigateToEditTaskEvent = _navigateToEditTaskEvent;
        _navigateToPomodoroEvent = new SingleLiveEvent<>();
        navigateToPomodoroEvent = _navigateToPomodoroEvent;
    }

    private CombinedPagerTrigger buildTrigger() {
        Integer page = _currentPageLiveData.getValue();
        Set<Tag> tags = _selectedTagsLiveData.getValue();
        TaskSortOption sort = _sortOptionLiveData.getValue();
        Set<TaskFilterOption> filters = _filterOptionsLiveData.getValue();

        if (page == null || tags == null || sort == null || filters == null) {
            // Возвращаем дефолтный триггер или null, если какие-то значения еще не установлены
            // Это важно, чтобы избежать NPE при первой инициализации MediatorLiveData
            return new CombinedPagerTrigger(INITIAL_PAGE, Collections.emptySet(), TaskSortOption.TIME_ASC, Collections.singleton(TaskFilterOption.ALL));
        }
        return new CombinedPagerTrigger(page, tags, sort, filters);
    }

    public void clearRequestedTaskDetails() {
        _requestedTaskIdForDetails.setValue(-1L); // Или null
    }

    public void clearNavigateToEditTask() {
        _navigateToEditTaskEvent.setValue(null);
    }

    public void clearNavigateToPomodoro() {
        _navigateToPomodoroEvent.setValue(null);
    }


    private CalendarDashboardData applyFiltersAndSorting(
            CalendarDashboardData data,
            Set<Tag> selectedTags,
            TaskSortOption sortOption,
            Set<TaskFilterOption> filterOptions) {
        if (data == null || data.getTasks() == null) return CalendarDashboardData.EMPTY;

        List<CalendarTaskSummary> tasksToProcess = new ArrayList<>(data.getTasks());

        final boolean hasTagFilter = selectedTags != null && !selectedTags.isEmpty();
        final boolean isAllFilterSelected = filterOptions.contains(TaskFilterOption.ALL);
        final boolean onlyComplete = filterOptions.contains(TaskFilterOption.COMPLETE);
        final boolean onlyIncomplete = filterOptions.contains(TaskFilterOption.INCOMPLETE);
        final boolean criticalPriorityFilter = filterOptions.contains(TaskFilterOption.CRITICAL_PRIORITY);
        final boolean highPriorityFilter = filterOptions.contains(TaskFilterOption.HIGH_PRIORITY);
        // Добавьте фильтры TODAY и OVERDUE если они релевантны для этого экрана
        // final boolean todayFilter = filterOptions.contains(TaskFilterOption.TODAY);
        // final boolean overdueFilter = filterOptions.contains(TaskFilterOption.OVERDUE);
        // final LocalDate todayDate = dateTimeUtils.currentLocalDate();

        List<CalendarTaskSummary> filteredTasks = tasksToProcess.stream()
                .filter(task -> {
                    if (hasTagFilter && task.getTags().stream().noneMatch(selectedTags::contains)) {
                        return false;
                    }
                    if (isAllFilterSelected) return true;

                    boolean passesCompletion = true;
                    if (onlyComplete != onlyIncomplete) {
                        passesCompletion = onlyComplete ? (task.getStatus() == TaskStatus.DONE) : (task.getStatus() != TaskStatus.DONE);
                    }
                    if (!passesCompletion) return false;

                    if (criticalPriorityFilter && task.getPriority() != Priority.CRITICAL) return false;
                    if (highPriorityFilter && task.getPriority() != Priority.HIGH && task.getPriority() != Priority.CRITICAL) return false;

                    // if (todayFilter && !task.getDueDate().toLocalDate().isEqual(todayDate)) return false;
                    // if (overdueFilter && (!task.getDueDate().toLocalDate().isBefore(todayDate) || task.getStatus() == TaskStatus.DONE)) return false;

                    return true;
                })
                .collect(Collectors.toList());

        switch (sortOption) {
            case TIME_ASC: filteredTasks.sort(Comparator.comparing(CalendarTaskSummary::getDueDate)); break;
            case TIME_DESC: filteredTasks.sort(Comparator.comparing(CalendarTaskSummary::getDueDate).reversed()); break;
            case CREATED_NEWEST: filteredTasks.sort(Comparator.comparingLong(CalendarTaskSummary::getId).reversed()); break;
            case CREATED_OLDEST: filteredTasks.sort(Comparator.comparingLong(CalendarTaskSummary::getId)); break;
            case PRIORITY_DESC: filteredTasks.sort(Comparator.comparing((CalendarTaskSummary t) -> t.getPriority().ordinal()).reversed()); break;
            case PRIORITY_ASC: filteredTasks.sort(Comparator.comparing((CalendarTaskSummary t) -> t.getPriority().ordinal())); break;
            case STATUS: filteredTasks.sort(Comparator.comparing(t -> t.getStatus() == TaskStatus.DONE)); break;
        }
        return new CalendarDashboardData(filteredTasks, data.getGamification());
    }

    public void updateSortOption(TaskSortOption option) { _sortOptionLiveData.setValue(option); }

    public void toggleFilterOption(TaskFilterOption option) {
        Set<TaskFilterOption> currentOptions = new HashSet<>(Objects.requireNonNull(_filterOptionsLiveData.getValue()));
        Set<TaskFilterOption> newOptions;
        if (option == TaskFilterOption.ALL) {
            newOptions = Collections.singleton(TaskFilterOption.ALL);
        } else if (currentOptions.contains(TaskFilterOption.ALL)) {
            newOptions = Collections.singleton(option);
        } else if (currentOptions.contains(option)) {
            currentOptions.remove(option);
            newOptions = currentOptions.isEmpty() ? Collections.singleton(TaskFilterOption.ALL) : currentOptions;
        } else {
            currentOptions.remove(TaskFilterOption.ALL);
            currentOptions.add(option);
            newOptions = currentOptions;
        }
        _filterOptionsLiveData.setValue(newOptions);
    }
    public void resetSortAndFilters() {
        _sortOptionLiveData.setValue(TaskSortOption.TIME_ASC);
        _filterOptionsLiveData.setValue(Collections.singleton(TaskFilterOption.ALL));
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

    public void clearSnackbarMessage() { _snackbarMessageEvent.setValue(null); }

    public void onPageChanged(int page) {
        if (Objects.equals(_currentPageLiveData.getValue(), page)) return;
        if (page >= 0 && page < pageDates.size()) {
            _currentPageLiveData.setValue(page);
            _selectedDateLiveData.setValue(getDateForPageInternal(page));
        } else {
            logger.warn(TAG, "Attempted to change to invalid page index: " + page);
        }
    }

    public void selectDate(LocalDateTime newDate) {
        LocalDate today = dateTimeUtils.currentLocalDate();
        LocalDate targetDate = newDate.toLocalDate();
        long daysDifference = ChronoUnit.DAYS.between(today, targetDate);
        int targetPage = INITIAL_PAGE + (int)daysDifference;

        if (targetPage >= 0 && targetPage < pageDates.size()) {
            LocalDateTime currentSelected = _selectedDateLiveData.getValue();
            if (currentSelected != null && Objects.equals(targetDate, currentSelected.toLocalDate())) return;

            _selectedDateLiveData.setValue(newDate.with(LocalTime.NOON));
            _currentPageLiveData.setValue(targetPage); // Обновляем страницу, чтобы пейджер тоже среагировал
        } else {
            logger.warn(TAG, "Selected date " + targetDate + " is outside the pager range (" + targetPage + ").");
            showSnackbar("Выбранная дата вне доступного диапазона.");
        }
    }

    private LocalDateTime getDateForPageInternal(int page) {
        if (page >= 0 && page < pageDates.size()) {
            return pageDates.get(page);
        }
        return dateTimeUtils.currentLocalDateTime().plusDays((long)page - INITIAL_PAGE);
    }
    public LocalDateTime getDateForPage(int page) { return getDateForPageInternal(page); }

    public void showCalendarDialog() { _showCalendarDialogLiveData.setValue(true); }
    public void hideCalendarDialog() { _showCalendarDialogLiveData.setValue(false); }

    public void updateTaskStatus(long taskId, List<Tag> tags, boolean isDone) {
        logger.debug(TAG, "Updating task status: taskId=" + taskId + ", isDone=" + isDone);
        ListenableFuture<Void> future;
        if (isDone) {
            future = processTaskCompletionUseCase.execute(taskId, tags);
        } else {
            future = markTaskAsIncompleteUseCase.execute(taskId);
        }
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) { /* UI обновится через LiveData */ }
            @Override
            public void onFailure(@NonNull Throwable t) {
                String message = "Ошибка обновления статуса: " + t.getMessage();
                showSnackbar(message);
                logger.error(TAG, "Failed to update task status for " + taskId, t);
            }
        }, ioExecutor);
    }

    private void requestDeleteConfirmation(CalendarTaskSummary taskSummary) {
        _swipeActionStateLiveData.setValue(new SwipeActionState.ConfirmingDelete(taskSummary));
    }

    public void confirmDeleteTask(long taskId) {
        logger.debug(TAG, "Deleting task confirmed: taskId=" + taskId);
        _swipeActionStateLiveData.setValue(SwipeActionState.Idle.getInstance());
        ListenableFuture<Void> future = deleteTaskUseCase.execute(taskId);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) { showSnackbar("Задача удалена"); }
            @Override public void onFailure(@NonNull Throwable t) {
                showSnackbar("Ошибка удаления: " + t.getMessage());
                logger.error(TAG, "Failed to delete task " + taskId, t);
            }
        }, ioExecutor);
    }

    public void clearSwipeActionState() {
        _swipeActionStateLiveData.setValue(SwipeActionState.Idle.getInstance());
    }

    public void handleSwipeAction(long taskId, SwipeDirection direction) {
        CalendarDashboardData currentData = dashboardDataLiveData.getValue();
        if (currentData == null || currentData.getTasks() == null) {
            logger.error(TAG, "Cannot handle swipe: dashboard data not available.");
            return;
        }
        CalendarTaskSummary taskToActOn = currentData.getTasks().stream()
                .filter(t -> t.getId() == taskId).findFirst().orElse(null);
        if (taskToActOn == null) {
            logger.error(TAG, "Cannot handle swipe: task " + taskId + " not found.");
            return;
        }

        if (direction == SwipeDirection.RIGHT) {
            boolean targetStatusIsDone = taskToActOn.getStatus() != TaskStatus.DONE;
            updateTaskStatus(taskId, taskToActOn.getTags(), targetStatusIsDone);
        } else if (direction == SwipeDirection.LEFT) {
            requestDeleteConfirmation(taskToActOn);
        }
    }

    // --- Методы для BottomSheet ---
    public void showTaskDetailsBottomSheet(CalendarTaskSummary task) {
        _requestedTaskIdForDetails.setValue(task.getId());
        // Показ BottomSheet будет управляться из Fragment'а, наблюдающего за taskDetailsForBottomSheetLiveData
    }

    /**
     * Получает LiveData для задачи, детали которой нужно показать в BottomSheet.
     * Fragment подписывается на это LiveData.
     */
    public LiveData<CalendarTaskSummary> getTaskDetailsForBottomSheet(long taskId) {
        _requestedTaskIdForDetails.setValue(taskId); // Устанавливаем ID запрашиваемой задачи
        return taskDetailsForBottomSheetLiveData; // Возвращаем LiveData, которое обновится
    }

    public void startPomodoroForTask(long taskId) {
        logger.debug(TAG, "Requesting Pomodoro start for task ID: " + taskId);
        _navigateToPomodoroEvent.setValue(taskId);
    }

    public void editTask(long taskId) {
        logger.debug(TAG, "Requesting edit for task ID: " + taskId);
        _navigateToEditTaskEvent.setValue(taskId);
    }

    private void showSnackbar(String message) {
        snackbarManager.showMessage(message);
    }
}