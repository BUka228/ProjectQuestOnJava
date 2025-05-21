package com.example.projectquestonjava.approach.calendar.domain.usecases;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarDashboardData;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskWithTagsAndPomodoro;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarRepository;
import com.example.projectquestonjava.approach.calendar.extensions.CalendarExtensions;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class GetDashboardDataUseCase {
    private static final String TAG = "GetDashboardDataUseCase";

    private final CalendarRepository calendarRepository;
    private final GamificationRepository gamificationRepository;
    private final PriorityResolver priorityResolver;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor; // Используем Executor
    private final Logger logger;

    @Inject
    public GetDashboardDataUseCase(
            CalendarRepository calendarRepository,
            GamificationRepository gamificationRepository,
            PriorityResolver priorityResolver,
            WorkspaceSessionManager workspaceSessionManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.calendarRepository = calendarRepository;
        this.gamificationRepository = gamificationRepository;
        this.priorityResolver = priorityResolver;
        this.workspaceSessionManager = workspaceSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public LiveData<CalendarDashboardData> execute(LocalDateTime date) {
        logger.debug(TAG, "Invoked for date: " + date);

        // Используем Transformations.switchMap для реакции на изменение workspaceId
        return Transformations.switchMap(workspaceSessionManager.getWorkspaceIdLiveData(), workspaceId -> {
            if (workspaceId == null || workspaceId == 0L) {
                logger.warn(TAG, "No active workspace set, returning empty data.");
                return new LiveData<CalendarDashboardData>(CalendarDashboardData.EMPTY) {};
            }
            logger.debug(TAG, "Using workspaceId: " + workspaceId);

            LiveData<List<CalendarTaskWithTagsAndPomodoro>> tasksLiveData = calendarRepository.getTasksForDay(workspaceId, date);
            LiveData<Gamification> gamificationLiveData = gamificationRepository.getCurrentUserGamificationFlow();

            MediatorLiveData<CalendarDashboardData> resultLiveData = new MediatorLiveData<>();
            final List<CalendarTaskWithTagsAndPomodoro>[] tasksHolder = new List[1];
            final Gamification[] gamificationHolder = new Gamification[1];

            resultLiveData.addSource(tasksLiveData, tasks -> {
                tasksHolder[0] = tasks;
                if (gamificationHolder[0] != null || !gamificationRepository.getCurrentUserGamificationFlow().hasActiveObservers()) {
                    updateCombinedResult(resultLiveData, tasksHolder[0], gamificationHolder[0]);
                }
            });

            resultLiveData.addSource(gamificationLiveData, gamification -> {
                gamificationHolder[0] = gamification;
                // Обновляем, если задачи уже загружены или если это первое значение gamification
                if (tasksHolder[0] != null || !tasksLiveData.hasActiveObservers()) {
                    updateCombinedResult(resultLiveData, tasksHolder[0], gamificationHolder[0]);
                }
            });
            return resultLiveData;
        });
    }

    private void updateCombinedResult(
            MediatorLiveData<CalendarDashboardData> mediator,
            List<CalendarTaskWithTagsAndPomodoro> tasks,
            Gamification gamification
    ) {
        // Выполняем маппинг на IO потоке, если он ресурсоемкий
        ioExecutor.execute(() -> {
            try {
                List<CalendarTaskSummary> summaries = (tasks != null)
                        ? CalendarExtensions.toTaskSummaries(tasks, priorityResolver, dateTimeUtils)
                        : Collections.emptyList();

                logger.debug(TAG, "Combining data: " + summaries.size() + " tasks, gamification loaded: " + (gamification != null));
                CalendarDashboardData data = new CalendarDashboardData(summaries, gamification);
                mediator.postValue(data);
            } catch (Exception e) {
                logger.error(TAG, "Error during data combination/mapping", e);
                mediator.postValue(CalendarDashboardData.EMPTY);
            }
        });
    }
}