package com.example.projectquestonjava.approach.calendar.domain.usecases;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskWithTagsAndPomodoro;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarRepository;
import com.example.projectquestonjava.approach.calendar.extensions.CalendarExtensions;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class GetTasksForMonthUseCase {
    private static final String TAG = "GetTasksForMonthUseCase";

    private final CalendarRepository calendarRepository;
    private final PriorityResolver priorityResolver;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Logger logger;

    @Inject
    public GetTasksForMonthUseCase(
            CalendarRepository calendarRepository,
            PriorityResolver priorityResolver,
            WorkspaceSessionManager workspaceSessionManager,
            DateTimeUtils dateTimeUtils,
            Logger logger) {
        this.calendarRepository = calendarRepository;
        this.priorityResolver = priorityResolver;
        this.workspaceSessionManager = workspaceSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.logger = logger;
    }

    public LiveData<List<CalendarTaskSummary>> execute(LocalDate monthStartDate) {
        logger.debug(TAG, "Invoked for month starting: " + monthStartDate);

        return Transformations.switchMap(workspaceSessionManager.getWorkspaceIdLiveData(), workspaceId -> {
            if (workspaceId == null || workspaceId == 0L) {
                logger.warn(TAG, "No active workspace set.");
                return new LiveData<>(Collections.emptyList()) {
                };
            }
            logger.debug(TAG, "Fetching tasks for workspaceId=" + workspaceId + ", month=" + monthStartDate);

            LiveData<List<CalendarTaskWithTagsAndPomodoro>> tasksWithDetailsLiveData =
                    calendarRepository.getTasksForMonth(workspaceId, monthStartDate);

            return Transformations.map(tasksWithDetailsLiveData, tasksWithDetails -> {
                // Это преобразование может быть ресурсоемким, лучше вынести на ioExecutor,
                // но Transformations.map выполняется на главном потоке.
                if (tasksWithDetails == null) return Collections.emptyList();
                try {
                    return CalendarExtensions.toTaskSummaries(tasksWithDetails, priorityResolver, dateTimeUtils);
                } catch (Exception e) {
                    logger.error(TAG, "Error mapping tasks to summaries for month " + monthStartDate, e);
                    return Collections.emptyList();
                }
            });
        });
    }
}