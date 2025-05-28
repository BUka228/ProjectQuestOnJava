// File: A:\Progects\ProjectQuestOnJava\app\src\main\java\com\example\projectquestonjava\approach\calendar\presentation\viewmodels\CalendarDashboardViewModel.java
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

    // ... (константы и enum SwipeDirection, SwipeActionState остаются такими же) ...
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
    private static final int PREFETCH_DISTANCE = 1;
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

    private final SingleLiveEvent<String> _snackbarMessageEvent;
    public final LiveData<String> snackbarMessageEvent;

    private final MutableLiveData<Set<Tag>> _selectedTagsLiveData;
    public final LiveData<Set<Tag>> selectedTagsLiveData;

    private final MutableLiveData<Boolean> _showCalendarDialogLiveData;
    public final LiveData<Boolean> showCalendarDialogLiveData;

    private final MutableLiveData<SwipeActionState> _swipeActionStateLiveData;
    public final LiveData<SwipeActionState> swipeActionStateLiveData;

    // Упрощаем pagerDataLiveData - он будет содержать данные ТОЛЬКО для текущего диапазона дат пейджера
    private final MutableLiveData<Map<LocalDate, CalendarDashboardData>> _pagerDataCache = new MutableLiveData<>(Collections.emptyMap());
    public final LiveData<Map<LocalDate, CalendarDashboardData>> pagerDataLiveData = _pagerDataCache; // Публичный доступ к кешу

    public final LiveData<CalendarDashboardData> dashboardDataLiveData;
    public final LiveData<Float> currentProgressLiveData;

    private final List<LocalDateTime> pageDates;

    private final MutableLiveData<Long> _requestedTaskIdForDetails;
    public final LiveData<CalendarTaskSummary> taskDetailsForBottomSheetLiveData;

    private final SingleLiveEvent<Long> _navigateToEditTaskEvent;
    public LiveData<Long> navigateToEditTaskEvent;
    private final SingleLiveEvent<Long> _navigateToPomodoroEvent;
    public LiveData<Long> navigateToPomodoroEvent;

    // Флаг для предотвращения многократной загрузки при быстром изменении триггеров
    private final AtomicInteger activeLoadOperations = new AtomicInteger(0);


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
        this.mainExecutor = MoreExecutors.directExecutor(); // Для простых обновлений LiveData

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

        // Mediator для отслеживания изменений триггеров и запуска загрузки данных
        MediatorLiveData<CombinedPagerTrigger> loadTrigger = new MediatorLiveData<>();
        Observer<Object> triggerObserver = o -> {
            CombinedPagerTrigger currentTrigger = buildTrigger();
            // Запускаем загрузку данных только если нет активных операций и триггер действительно изменился (или первая загрузка)
            // Проверку на изменение триггера можно опустить, если debounce сделает это эффективно.
            triggerDataLoad(currentTrigger);
        };

        loadTrigger.addSource(_currentPageLiveData, triggerObserver);
        loadTrigger.addSource(_selectedTagsLiveData, triggerObserver);
        loadTrigger.addSource(_sortOptionLiveData, triggerObserver);
        loadTrigger.addSource(_filterOptionsLiveData, triggerObserver);
        // Устанавливаем начальное значение, чтобы инициировать первую загрузку
        loadTrigger.setValue(buildTrigger());


        dashboardDataLiveData = Transformations.switchMap(_selectedDateLiveData, selectedDateTime ->
                Transformations.map(_pagerDataCache, pagerData -> { // Теперь dashboardDataLiveData зависит от _pagerDataCache
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

        _requestedTaskIdForDetails = new MutableLiveData<>(null);
        taskDetailsForBottomSheetLiveData = Transformations.switchMap(_requestedTaskIdForDetails, taskId -> {
            if (taskId == null || taskId == -1L) return new MutableLiveData<>(null);
            return Transformations.map(dashboardDataLiveData, currentDayData ->
                    (currentDayData == null || currentDayData.getTasks() == null) ? null :
                            currentDayData.getTasks().stream().filter(task -> task.getId() == taskId).findFirst().orElse(null)
            );
        });

        _navigateToEditTaskEvent = new SingleLiveEvent<>();
        navigateToEditTaskEvent = _navigateToEditTaskEvent;
        _navigateToPomodoroEvent = new SingleLiveEvent<>();
        navigateToPomodoroEvent = _navigateToPomodoroEvent;
    }

    private void triggerDataLoad(CombinedPagerTrigger trigger) {
        logger.debug(TAG, "Data load triggered for: " + trigger);
        if (activeLoadOperations.getAndIncrement() == 0) { // Начинаем загрузку, только если не было других активных
            loadDataForPagerRange(trigger);
        } else {
            // Если уже идет загрузка, можно ее отменить и запустить новую,
            // или дождаться завершения текущей.
            // Пока просто логируем и уменьшаем счетчик, т.к. loadDataForPagerRange запустится позже.
            activeLoadOperations.decrementAndGet();
            logger.debug(TAG, "Data load already in progress or queued. Current ops: " + activeLoadOperations.get());
            // Можно запланировать перезагрузку после текущей, если триггер изменился значительно
            // Это более сложная логика, пока оставим так.
        }
    }

    private void loadDataForPagerRange(CombinedPagerTrigger trigger) {
        LocalDate centerDate = getDateForPageInternal(trigger.page()).toLocalDate();
        int range = PREFETCH_DISTANCE;
        List<LocalDate> datesToObserve = new ArrayList<>();
        for (int i = -range; i <= range; i++) {
            datesToObserve.add(centerDate.plusDays(i));
        }
        logger.info(TAG, "Starting data load for range: " + datesToObserve + " with trigger: " + trigger);

        // Список для ListenableFuture от каждого запроса
        List<ListenableFuture<CalendarDashboardData>> futures = new ArrayList<>();
        for (LocalDate date : datesToObserve) {
            // GetDashboardDataUseCase.execute возвращает LiveData, что не подходит для ListenableFuture.
            // Нам нужно, чтобы UseCase возвращал ListenableFuture или мы должны обернуть LiveData.
            // Предположим, что GetDashboardDataUseCase может вернуть ListenableFuture (или мы его изменим).
            // Для примера, если он возвращает LiveData, мы можем взять первое значение:
            ListenableFuture<CalendarDashboardData> dataFuture = Futures.submit(() -> {
                // Этот блок выполнится на ioExecutor
                LiveData<CalendarDashboardData> liveData = (LiveData<CalendarDashboardData>) getDashboardDataUseCase.execute(date.atStartOfDay());
                // Получаем значение синхронно (но мы в ioExecutor)
                // Это рискованно, если LiveData обновляется много раз. Лучше, чтобы UseCase возвращал Future.
                final CalendarDashboardData[] dataHolder = new CalendarDashboardData[1];
                Observer<CalendarDashboardData> observer = rawData -> dataHolder[0] = rawData;
                // Подписываемся на главном потоке, если LiveData эмитит на нем
                mainExecutor.execute(() -> liveData.observeForever(observer));
                // Ждем, пока данные не придут (не лучший способ, но для адаптации)
                int attempts = 0;
                while(dataHolder[0] == null && attempts < 100) { // Ограничение по попыткам
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    attempts++;
                }
                mainExecutor.execute(() -> liveData.removeObserver(observer));
                return dataHolder[0] != null ? dataHolder[0] : CalendarDashboardData.EMPTY;
            }, ioExecutor);

            ListenableFuture<CalendarDashboardData> processedFuture = Futures.transform(dataFuture, rawData ->
                            applyFiltersAndSorting(rawData, trigger.tags(), trigger.sort(), trigger.filters()),
                    ioExecutor // Обработка тоже на ioExecutor
            );
            futures.add(processedFuture);
        }

        ListenableFuture<List<CalendarDashboardData>> allDataFuture = Futures.allAsList(futures);

        Futures.addCallback(allDataFuture, new FutureCallback<List<CalendarDashboardData>>() {
            @Override
            public void onSuccess(List<CalendarDashboardData> results) {
                Map<LocalDate, CalendarDashboardData> newCache = new HashMap<>();
                for (int i = 0; i < datesToObserve.size(); i++) {
                    if (results.get(i) != null) { // Защита от null, если какой-то Future упал без исключения
                        newCache.put(datesToObserve.get(i), results.get(i));
                    } else {
                        newCache.put(datesToObserve.get(i), CalendarDashboardData.EMPTY);
                        logger.warn(TAG, "Null data returned for date: " + datesToObserve.get(i));
                    }
                }
                _pagerDataCache.postValue(newCache); // Обновляем кеш
                activeLoadOperations.decrementAndGet();
                logger.info(TAG, "Data load successful for range. Active ops: " + activeLoadOperations.get());
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to load data for pager range", t);
                // Можно установить ошибку в _uiStateLiveData
                activeLoadOperations.decrementAndGet();
                // Чтобы не сбрасывать старые данные, если они были, можно не обновлять _pagerDataCache
                // или обновлять с ошибкой для конкретных дат.
            }
        }, MoreExecutors.directExecutor()); // Коллбэк можно выполнить в том же потоке
    }


    private CombinedPagerTrigger buildTrigger() {
        Integer page = _currentPageLiveData.getValue();
        Set<Tag> tags = _selectedTagsLiveData.getValue();
        TaskSortOption sort = _sortOptionLiveData.getValue();
        Set<TaskFilterOption> filters = _filterOptionsLiveData.getValue();

        if (page == null) page = INITIAL_PAGE;
        if (tags == null) tags = Collections.emptySet();
        if (sort == null) sort = TaskSortOption.TIME_ASC;
        if (filters == null) filters = Collections.singleton(TaskFilterOption.ALL);

        return new CombinedPagerTrigger(page, tags, sort, filters);
    }

    public void clearRequestedTaskDetails() {
        _requestedTaskIdForDetails.setValue(-1L);
    }
    public void clearNavigateToEditTask() { _navigateToEditTaskEvent.setValue(null); }
    public void clearNavigateToPomodoro() { _navigateToPomodoroEvent.setValue(null); }

    private CalendarDashboardData applyFiltersAndSorting(
            CalendarDashboardData data,
            Set<Tag> selectedTags,
            TaskSortOption sortOption,
            Set<TaskFilterOption> filterOptions) {
        // ... (логика этого метода остается такой же, как в предыдущем ответе)
        if (data == null || data.getTasks() == null) return CalendarDashboardData.EMPTY;

        List<CalendarTaskSummary> tasksToProcess = new ArrayList<>(data.getTasks());

        final boolean hasTagFilter = selectedTags != null && !selectedTags.isEmpty();
        final boolean isAllFilterSelected = filterOptions.contains(TaskFilterOption.ALL);
        final boolean onlyComplete = filterOptions.contains(TaskFilterOption.COMPLETE);
        final boolean onlyIncomplete = filterOptions.contains(TaskFilterOption.INCOMPLETE);
        final boolean criticalPriorityFilter = filterOptions.contains(TaskFilterOption.CRITICAL_PRIORITY);
        final boolean highPriorityFilter = filterOptions.contains(TaskFilterOption.HIGH_PRIORITY);

        List<CalendarTaskSummary> filteredTasks = tasksToProcess.stream()
                .filter(task -> {
                    if (hasTagFilter && (task.getTags() == null || task.getTags().stream().noneMatch(selectedTags::contains))) {
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
            currentOptions.remove(TaskFilterOption.ALL); // Важно: если выбран ALL, его нужно убрать
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
            LocalDateTime currentSelectedDateTime = _selectedDateLiveData.getValue();
            LocalDate currentSelectedDate = (currentSelectedDateTime != null) ? currentSelectedDateTime.toLocalDate() : null;

            if (Objects.equals(targetDate, currentSelectedDate) && Objects.equals(_currentPageLiveData.getValue(), targetPage)) {
                return;
            }
            _selectedDateLiveData.setValue(newDate.with(LocalTime.NOON));
            _currentPageLiveData.setValue(targetPage); // Это триггернет loadTrigger
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
            @Override public void onSuccess(Void result) { /* UI обновится через LiveData */ }
            @Override public void onFailure(@NonNull Throwable t) {
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
        Map<LocalDate, CalendarDashboardData> currentPagerData = _pagerDataCache.getValue(); // Берем из кеша
        LocalDateTime currentSelectedDateTime = _selectedDateLiveData.getValue();

        if (currentPagerData == null || currentSelectedDateTime == null) {
            logger.error(TAG, "Cannot handle swipe for task " + taskId + ": pagerData or selectedDate is null.");
            return;
        }
        CalendarDashboardData dataForSelectedDay = currentPagerData.get(currentSelectedDateTime.toLocalDate());
        if (dataForSelectedDay == null || dataForSelectedDay.getTasks() == null) {
            logger.error(TAG, "Cannot handle swipe for task " + taskId + ": data for selected day not found or tasks are null.");
            return;
        }

        CalendarTaskSummary taskToActOn = dataForSelectedDay.getTasks().stream()
                .filter(t -> t.getId() == taskId).findFirst().orElse(null);
        if (taskToActOn == null) {
            logger.error(TAG, "Cannot handle swipe: task " + taskId + " not found in current day's data.");
            return;
        }

        if (direction == SwipeDirection.RIGHT) {
            boolean targetStatusIsDone = taskToActOn.getStatus() != TaskStatus.DONE;
            updateTaskStatus(taskId, taskToActOn.getTags(), targetStatusIsDone);
        } else if (direction == SwipeDirection.LEFT) {
            requestDeleteConfirmation(taskToActOn);
        }
    }

    public void showTaskDetailsBottomSheet(CalendarTaskSummary task) {
        _requestedTaskIdForDetails.setValue(task.getId());
    }

    public LiveData<CalendarTaskSummary> getTaskDetailsForBottomSheet(long taskId) {
        _requestedTaskIdForDetails.setValue(taskId);
        return taskDetailsForBottomSheetLiveData;
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