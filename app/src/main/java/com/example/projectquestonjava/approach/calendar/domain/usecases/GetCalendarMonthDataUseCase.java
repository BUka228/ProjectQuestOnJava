package com.example.projectquestonjava.approach.calendar.domain.usecases;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarMonthData;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskWithTagsAndPomodoro;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarRepository;
import com.example.projectquestonjava.approach.calendar.extensions.CalendarExtensions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class GetCalendarMonthDataUseCase {
    private static final String TAG = "GetCalendarMonthDataUseCase";

    private final CalendarRepository calendarRepository;
    private final PriorityResolver priorityResolver;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor; // Для выполнения маппинга в фоне
    private final Logger logger;

    @Inject
    public GetCalendarMonthDataUseCase(
            CalendarRepository calendarRepository,
            PriorityResolver priorityResolver,
            WorkspaceSessionManager workspaceSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.calendarRepository = calendarRepository;
        this.priorityResolver = priorityResolver;
        this.workspaceSessionManager = workspaceSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public LiveData<CalendarMonthData> execute(LocalDate monthStartDate) {
        logger.debug(TAG, "Invoked for month starting: " + monthStartDate);

        return Transformations.switchMap(workspaceSessionManager.getWorkspaceIdLiveData(), workspaceId -> {
            if (workspaceId == null || workspaceId == 0L) {
                logger.warn(TAG, "No active workspace set.");
                return new LiveData<CalendarMonthData>(CalendarMonthData.EMPTY) {};
            }
            logger.debug(TAG, "Fetching month data for workspaceId=" + workspaceId + ", month=" + monthStartDate);

            LiveData<List<CalendarTaskWithTagsAndPomodoro>> tasksWithDetailsLiveData =
                    calendarRepository.getTasksForMonth(workspaceId, monthStartDate);

            // Используем MediatorLiveData для асинхронного маппинга, если tasksWithDetailsLiveData обновляется
            MediatorLiveData<CalendarMonthData> resultLiveData = new MediatorLiveData<>();
            resultLiveData.setValue(CalendarMonthData.EMPTY); // Начальное значение

            resultLiveData.addSource(tasksWithDetailsLiveData, tasksWithDetails -> {
                if (tasksWithDetails == null) {
                    resultLiveData.setValue(CalendarMonthData.EMPTY);
                    return;
                }
                // Выполняем маппинг в фоновом потоке
                ioExecutor.execute(() -> {
                    try {
                        List<CalendarTaskSummary> taskSummaries = CalendarExtensions.toTaskSummaries(tasksWithDetails, priorityResolver, dateTimeUtils);

                        Map<LocalDate, Integer> dailyCounts = tasksWithDetails.stream()
                                .collect(Collectors.groupingBy(
                                        taskDetail -> dateTimeUtils.utcToLocalLocalDateTime(taskDetail.getTask().getDueDate()).toLocalDate(),
                                        Collectors.summingInt(taskDetail -> 1)
                                ));

                        resultLiveData.postValue(new CalendarMonthData(taskSummaries, dailyCounts));
                    } catch (Exception e) {
                        logger.error(TAG, "Error mapping data for month " + monthStartDate, e);
                        resultLiveData.postValue(CalendarMonthData.EMPTY); // В случае ошибки возвращаем пустое состояние
                    }
                });
            });
            return resultLiveData;
        });
    }
}