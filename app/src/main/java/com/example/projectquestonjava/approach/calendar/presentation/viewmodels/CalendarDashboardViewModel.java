package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
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
import com.example.projectquestonjava.core.utils.SingleLiveEvent; // Для Snackbar
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
import com.google.common.util.concurrent.MoreExecutors;

import dagger.hilt.android.lifecycle.HiltViewModel;
import lombok.Getter;

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
import java.util.concurrent.ConcurrentHashMap; // Для pagerDataCache
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

@HiltViewModel
public class CalendarDashboardViewModel extends ViewModel {

    public enum SwipeDirection { LEFT, RIGHT }

    public static abstract class SwipeActionState { // Эмуляция sealed class
        private SwipeActionState() {}
        public static final class Idle extends SwipeActionState {
            private static final Idle INSTANCE = new Idle();
            public static Idle getInstance() { return INSTANCE; }
        }
        @Getter
        public static final class ConfirmingDelete extends SwipeActionState {
            private final CalendarTaskSummary taskSummary;
            public ConfirmingDelete(CalendarTaskSummary taskSummary) { this.taskSummary = taskSummary; }
        }
    }

    public static final int DATE_RANGE = 30;
    private static final int INITIAL_PAGE = DATE_RANGE;
    private static final int PREFETCH_DISTANCE = 3; // Оставляем для логики pagerData
    private static final String TAG = "CalendarDashboardVM";

    private final ProcessTaskCompletionUseCase processTaskCompletionUseCase;
    private final MarkTaskAsIncompleteUseCase markTaskAsIncompleteUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final DateTimeUtils dateTimeUtils;
    private final SnackbarManager snackbarManager;
    private final Logger logger;

    private final MutableLiveData<LocalDateTime> _selectedDateLiveData = new MutableLiveData<>();
    public LiveData<LocalDateTime> selectedDateLiveData = _selectedDateLiveData;

    private final MutableLiveData<Integer> _currentPageLiveData = new MutableLiveData<>(INITIAL_PAGE);
    public LiveData<Integer> currentPageLiveData = _currentPageLiveData;

    private final MutableLiveData<TaskSortOption> _sortOptionLiveData = new MutableLiveData<>(TaskSortOption.TIME_ASC);
    public LiveData<TaskSortOption> sortOptionLiveData = _sortOptionLiveData;

    private final MutableLiveData<Set<TaskFilterOption>> _filterOptionsLiveData = new MutableLiveData<>(Collections.singleton(TaskFilterOption.ALL));
    public LiveData<Set<TaskFilterOption>> filterOptionsLiveData = _filterOptionsLiveData;

    private final SingleLiveEvent<String> _snackbarMessageEvent = new SingleLiveEvent<>();
    public LiveData<String> snackbarMessageEvent = _snackbarMessageEvent;

    private final MutableLiveData<Set<Tag>> _selectedTagsLiveData = new MutableLiveData<>(Collections.emptySet());
    public LiveData<Set<Tag>> selectedTagsLiveData = _selectedTagsLiveData;

    private final MutableLiveData<Boolean> _showCalendarDialogLiveData = new MutableLiveData<>(false);
    public LiveData<Boolean> showCalendarDialogLiveData = _showCalendarDialogLiveData;

    private final MutableLiveData<SwipeActionState> _swipeActionStateLiveData = new MutableLiveData<>(SwipeActionState.Idle.getInstance());
    public LiveData<SwipeActionState> swipeActionStateLiveData = _swipeActionStateLiveData;

    // --- LiveData для данных пейджера ---
    // Это будет сложнее, чем StateFlow.flatMapLatest.
    // Используем MediatorLiveData и Transformations.switchMap.
    private final MediatorLiveData<CombinedPagerTrigger> pagerTrigger = new MediatorLiveData<>();
    public final LiveData<Map<LocalDate, CalendarDashboardData>> pagerDataLiveData;

    // --- LiveData для данных текущей страницы ---
    public final LiveData<CalendarDashboardData> dashboardDataLiveData;

    // --- LiveData для прогресса ---
    public final LiveData<Float> currentProgressLiveData;

    private final List<LocalDateTime> pageDates; // Для быстрого доступа к датам страниц

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
        this.processTaskCompletionUseCase = processTaskCompletionUseCase;
        this.markTaskAsIncompleteUseCase = markTaskAsIncompleteUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.dateTimeUtils = dateTimeUtils;
        this.snackbarManager = snackbarManager;
        this.logger = logger;

        _selectedDateLiveData.setValue(dateTimeUtils.currentLocalDateTime());

        pageDates = IntStream.range(0, DATE_RANGE * 2 + 1)
                .mapToObj(offset -> dateTimeUtils.currentLocalDateTime().plusDays((long) offset - INITIAL_PAGE))
                .collect(Collectors.toList());

        pagerTrigger.addSource(_currentPageLiveData, value -> pagerTrigger.setValue(buildTrigger()));
        pagerTrigger.addSource(_selectedTagsLiveData, value -> pagerTrigger.setValue(buildTrigger()));
        pagerTrigger.addSource(_sortOptionLiveData, value -> pagerTrigger.setValue(buildTrigger()));
        pagerTrigger.addSource(_filterOptionsLiveData, value -> pagerTrigger.setValue(buildTrigger()));
        // Инициализируем триггер начальными значениями
        pagerTrigger.setValue(buildTrigger());


        pagerDataLiveData = Transformations.switchMap(pagerTrigger, trigger -> {
            if (trigger == null) {
                MutableLiveData<Map<LocalDate, CalendarDashboardData>> emptyData = new MutableLiveData<>();
                emptyData.setValue(Collections.emptyMap());
                return emptyData;
            }
            // Логика загрузки данных для диапазона дат вокруг текущей страницы
            LocalDate centerDate = getDateForPageInternal(trigger.page).toLocalDate();
            int range = PREFETCH_DISTANCE;
            List<LocalDate> datesToObserve = new ArrayList<>();
            for (int i = -range; i <= range; i++) {
                datesToObserve.add(centerDate.plusDays(i));
            }
            logger.debug(TAG, "PagerTrigger changed. Observing data for dates: " + datesToObserve + " based on page " + trigger.page);

            // Объединяем LiveData для каждой даты
            MediatorLiveData<Map<LocalDate, CalendarDashboardData>> combinedData = new MediatorLiveData<>();
            Map<LocalDate, CalendarDashboardData> resultMap = new ConcurrentHashMap<>(); // Потокобезопасная мапа

            // Для отслеживания, все ли источники LiveData ответили
            AtomicInteger sourcesPending = new AtomicInteger(datesToObserve.size());

            for (LocalDate date : datesToObserve) {
                // getDashboardDataUseCase.execute(date.atStartOfDay()) возвращает LiveData
                LiveData<CalendarDashboardData> singleDateLiveData = getDashboardDataUseCase.execute(date.atStartOfDay());

                combinedData.addSource(singleDateLiveData, rawData -> {
                    if (rawData != null) {
                        // Фильтрация и сортировка должны выполняться в фоновом потоке
                        // Здесь для простоты синхронно, но в реальном приложении это может быть проблемой
                        CalendarDashboardData processedData = applyFiltersAndSorting(rawData, trigger.tags, trigger.sort, trigger.filters);
                        resultMap.put(date, processedData);
                    } else {
                        resultMap.put(date, CalendarDashboardData.EMPTY);
                        logger.warn(TAG, "Received null rawData for date: " + date);
                    }

                    // Этот механизм не идеален, т.к. sourcesPending будет уменьшаться при каждом обновлении любого LiveData.
                    // Правильнее было бы отслеживать, для каких дат данные уже загружены.
                    // Но для демонстрации пока оставим так.
                    if (sourcesPending.decrementAndGet() <= 0 || resultMap.size() == datesToObserve.size()) {
                        combinedData.setValue(new ConcurrentHashMap<>(resultMap)); // Отправляем копию
                        sourcesPending.set(datesToObserve.size()); // Сбрасываем для следующего обновления
                    }
                });
            }
            return combinedData;
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
            return (float) completedCount / tasks.size();
        });
    }

    private CombinedPagerTrigger buildTrigger() {
        return new CombinedPagerTrigger(
                Objects.requireNonNull(_currentPageLiveData.getValue()),
                Objects.requireNonNull(_selectedTagsLiveData.getValue()),
                Objects.requireNonNull(_sortOptionLiveData.getValue()),
                Objects.requireNonNull(_filterOptionsLiveData.getValue())
        );
    }

    private static class CombinedPagerTrigger {
        final int page;
        final Set<Tag> tags;
        final TaskSortOption sort;
        final Set<TaskFilterOption> filters;
        CombinedPagerTrigger(int p, Set<Tag> t, TaskSortOption s, Set<TaskFilterOption> f) {
            page = p; tags = t; sort = s; filters = f;
        }
        // equals и hashCode важны для distinctUntilChanged в LiveData/Flow
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CombinedPagerTrigger that = (CombinedPagerTrigger) o;
            return page == that.page && Objects.equals(tags, that.tags) && sort == that.sort && Objects.equals(filters, that.filters);
        }
        @Override
        public int hashCode() {
            return Objects.hash(page, tags, sort, filters);
        }
    }


    private CalendarDashboardData applyFiltersAndSorting(
            CalendarDashboardData data,
            Set<Tag> selectedTags,
            TaskSortOption sortOption,
            Set<TaskFilterOption> filterOptions) {
        if (data == null || data.getTasks() == null) return CalendarDashboardData.EMPTY;

        boolean hasTagFilter = selectedTags != null && !selectedTags.isEmpty();
        boolean isAllFilter = filterOptions != null && filterOptions.contains(TaskFilterOption.ALL);

        List<CalendarTaskSummary> filteredTasks = data.getTasks().stream()
                .filter(task -> {
                    boolean passesTagFilter = !hasTagFilter || task.getTags().stream().anyMatch(selectedTags::contains);
                    if (!passesTagFilter) return false;

                    if (isAllFilter) return true; // Если "Все", то остальные фильтры статуса/приоритета не применяем так строго

                    boolean passesCompletionFilter = true;
                    if (filterOptions.contains(TaskFilterOption.COMPLETE) != filterOptions.contains(TaskFilterOption.INCOMPLETE)) { // Если выбран только один из них
                        if (filterOptions.contains(TaskFilterOption.COMPLETE)) passesCompletionFilter = task.getStatus() == TaskStatus.DONE;
                        else if (filterOptions.contains(TaskFilterOption.INCOMPLETE)) passesCompletionFilter = task.getStatus() != TaskStatus.DONE;
                    }
                    if (!passesCompletionFilter) return false;

                    boolean passesPriorityFilter = true;
                    if (filterOptions.contains(TaskFilterOption.CRITICAL_PRIORITY)) passesPriorityFilter = task.getPriority() == Priority.CRITICAL;
                    else if (filterOptions.contains(TaskFilterOption.HIGH_PRIORITY)) passesPriorityFilter = task.getPriority() == Priority.HIGH || task.getPriority() == Priority.CRITICAL;
                    // Если не выбраны фильтры по приоритету, то passesPriorityFilter остается true
                    return passesPriorityFilter;
                })
                .collect(Collectors.toList());

        // Сортировка
        switch (sortOption) {
            case TIME_ASC: filteredTasks.sort(Comparator.comparing(CalendarTaskSummary::getDueDate)); break;
            case TIME_DESC: filteredTasks.sort(Comparator.comparing(CalendarTaskSummary::getDueDate).reversed()); break;
            case CREATED_NEWEST: filteredTasks.sort(Comparator.comparingLong(CalendarTaskSummary::getId).reversed()); break;
            case CREATED_OLDEST: filteredTasks.sort(Comparator.comparingLong(CalendarTaskSummary::getId)); break;
            case PRIORITY_DESC: filteredTasks.sort(Comparator.comparing((CalendarTaskSummary t) -> t.getPriority().ordinal()).reversed()); break;
            case PRIORITY_ASC: filteredTasks.sort(Comparator.comparing((CalendarTaskSummary t) -> t.getPriority().ordinal())); break;
            case STATUS: filteredTasks.sort(Comparator.comparing(t -> t.getStatus() == TaskStatus.DONE)); break; // false (не DONE) будет раньше
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
            currentOptions.remove(TaskFilterOption.ALL); // Удаляем ALL, если он был
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
        if (page >= 0 && page < (DATE_RANGE * 2 + 1)) {
            _currentPageLiveData.setValue(page);
            _selectedDateLiveData.setValue(getDateForPageInternal(page));
            logger.debug(TAG, "Page changed to " + page + ", date set to " + Objects.requireNonNull(_selectedDateLiveData.getValue()).toLocalDate());
        } else {
            logger.warn(TAG, "Attempted to change to invalid page index: " + page);
        }
    }

    public void selectDate(LocalDateTime newDate) {
        LocalDate today = dateTimeUtils.currentLocalDate();
        LocalDate targetDate = newDate.toLocalDate();
        long daysDifference = ChronoUnit.DAYS.between(today, targetDate);
        int targetPage = INITIAL_PAGE + (int) daysDifference;

        if (targetPage >= 0 && targetPage < (DATE_RANGE * 2 + 1)) {
            if (Objects.equals(targetDate, Objects.requireNonNull(_selectedDateLiveData.getValue()).toLocalDate())) {
                logger.debug(TAG, "Date " + targetDate + " already selected.");
                return;
            }
            _selectedDateLiveData.setValue(newDate.with(LocalTime.NOON)); // Установка на полдень
            _currentPageLiveData.setValue(targetPage);
            logger.debug(TAG, "Date selected via calendar: " + targetDate + ", scrolling to page " + targetPage);
        } else {
            logger.warn(TAG, "Selected date " + targetDate + " is outside the pager range (" + targetPage + ").");
            showSnackbar("Выбранная дата вне доступного диапазона.");
        }
    }

    private LocalDateTime getDateForPageInternal(int page) {
        if (page >= 0 && page < pageDates.size()) {
            return pageDates.get(page);
        }
        logger.warn(TAG, "Requested page " + page + " is outside pre-generated range. Calculating dynamically.");
        return dateTimeUtils.currentLocalDateTime().plusDays((long) page - INITIAL_PAGE);
    }
    public LocalDateTime getDateForPage(int page) { return getDateForPageInternal(page); }

    public void showCalendarDialog() { _showCalendarDialogLiveData.setValue(true); }
    public void hideCalendarDialog() { _showCalendarDialogLiveData.setValue(false); }

    private void updateTaskStatusInternal(long taskId, List<Tag> tags, boolean isDone) {
        logger.debug(TAG, "Updating task status: taskId=" + taskId + ", isDone=" + isDone);
        ListenableFuture<Void> future;
        if (isDone) {
            future = processTaskCompletionUseCase.execute(taskId, tags);
        } else {
            future = markTaskAsIncompleteUseCase.execute(taskId);
        }
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) { /* Данные обновятся через LiveData */ }
            @Override
            public void onFailure(@NonNull Throwable t) {
                String message = "Ошибка обновления статуса: " + t.getMessage();
                showSnackbar(message);
                logger.error(TAG, "Failed to update task status for " + taskId, t);
            }
        }, MoreExecutors.directExecutor()); // Коллбэк на том же потоке (UI), если операция быстрая
    }

    private void requestDeleteConfirmation(CalendarTaskSummary taskSummary) {
        _swipeActionStateLiveData.setValue(new SwipeActionState.ConfirmingDelete(taskSummary));
    }

    public void confirmDeleteTask(long taskId) {
        logger.debug(TAG, "Deleting task confirmed: taskId=" + taskId);
        _swipeActionStateLiveData.setValue(SwipeActionState.Idle.getInstance());
        ListenableFuture<Void> future = deleteTaskUseCase.execute(taskId);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) { showSnackbar("Задача удалена"); }
            @Override
            public void onFailure(@NonNull Throwable t) {
                String message = "Ошибка удаления задачи: " + t.getMessage();
                showSnackbar(message);
                logger.error(TAG, "Failed to delete task " + taskId, t);
            }
        }, MoreExecutors.directExecutor());
    }

    public void clearSwipeActionState() {
        _swipeActionStateLiveData.setValue(SwipeActionState.Idle.getInstance());
    }

    public void handleSwipeAction(long taskId, SwipeDirection direction) {
        // Получение актуальных данных задачи перед действием.
        // Это может быть сложно с LiveData без блокировки.
        // Проще всего, если dashboardDataLiveData содержит актуальные данные.
        CalendarDashboardData currentData = dashboardDataLiveData.getValue();
        if (currentData == null || currentData.getTasks() == null) {
            logger.error(TAG, "Cannot handle swipe for task " + taskId + ": dashboard data not available.");
            return;
        }
        CalendarTaskSummary taskToActOn = currentData.getTasks().stream()
                .filter(t -> t.getId() == taskId)
                .findFirst()
                .orElse(null);

        if (taskToActOn == null) {
            logger.error(TAG, "Cannot handle swipe for task " + taskId + ": task not found in current data.");
            return;
        }

        if (direction == SwipeDirection.RIGHT) {
            logger.debug(TAG, "Handling RIGHT swipe for task " + taskId + ", actual current status " + taskToActOn.getStatus());
            boolean targetStatusIsDone = taskToActOn.getStatus() != TaskStatus.DONE;
            updateTaskStatusInternal(taskId, taskToActOn.getTags(), targetStatusIsDone);
        } else if (direction == SwipeDirection.LEFT) {
            logger.debug(TAG, "Handling LEFT swipe for task " + taskId + ", requesting delete confirmation.");
            requestDeleteConfirmation(taskToActOn);
        }
    }

    private void showSnackbar(String message) {
        // Теперь вызываем Java-метод, который не suspend
        snackbarManager.showMessage(message); // Использует длительность по умолчанию (LENGTH_SHORT)
    }

    private void showSnackbar(String message, int duration) { // Перегруженный метод, если нужна другая длительность
        snackbarManager.showMessage(message, duration);
    }
}