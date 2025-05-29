package com.example.projectquestonjava.approach.calendar.domain.usecases;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
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
import javax.inject.Inject;

public class GetDashboardDataUseCase {
    private static final String TAG = "GetDashboardDataUseCase";

    private final CalendarRepository calendarRepository;
    private final GamificationRepository gamificationRepository;
    private final PriorityResolver priorityResolver;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final DateTimeUtils dateTimeUtils;
    private final Logger logger;

    @Inject
    public GetDashboardDataUseCase(
            CalendarRepository calendarRepository,
            GamificationRepository gamificationRepository,
            PriorityResolver priorityResolver,
            WorkspaceSessionManager workspaceSessionManager,
            DateTimeUtils dateTimeUtils,
            Logger logger) {
        this.calendarRepository = calendarRepository;
        this.gamificationRepository = gamificationRepository;
        this.priorityResolver = priorityResolver;
        this.workspaceSessionManager = workspaceSessionManager;
        this.dateTimeUtils = dateTimeUtils;
        this.logger = logger;
    }

    public LiveData<CalendarDashboardData> execute(LocalDateTime date) {
        logger.debug(TAG, "execute: Called for date: " + date.toLocalDate());

        // LiveData для workspaceId
        LiveData<Long> workspaceIdLiveData = workspaceSessionManager.getWorkspaceIdLiveData();

        // Используем switchMap для реакции на изменение workspaceId
        return Transformations.switchMap(workspaceIdLiveData, workspaceId -> {
            if (workspaceId == null || workspaceId == 0L) {
                logger.warn(TAG, "execute: No active workspace set for date " + date.toLocalDate() + ". Returning EMPTY data.");
                MutableLiveData<CalendarDashboardData> emptyData = new MutableLiveData<>();
                emptyData.setValue(CalendarDashboardData.EMPTY);
                return emptyData;
            }
            logger.debug(TAG, "execute: Using workspaceId: " + workspaceId + " for date: " + date.toLocalDate());

            // Получаем LiveData задач и геймификации
            LiveData<List<CalendarTaskWithTagsAndPomodoro>> tasksWithDetailsLiveData =
                    calendarRepository.getTasksForDay(workspaceId, date);
            LiveData<Gamification> gamificationLiveData =
                    gamificationRepository.getCurrentUserGamificationFlow();

            // Объединяем их с помощью MediatorLiveData
            MediatorLiveData<CalendarDashboardData> result = new MediatorLiveData<>();
            result.setValue(CalendarDashboardData.EMPTY); // Начальное значение

            final List<CalendarTaskWithTagsAndPomodoro>[] tasksHolder = new List[1];
            final Gamification[] gamificationHolder = new Gamification[1];
            final boolean[] tasksLoaded = {false};
            final boolean[] gamificationLoaded = {false};

            Runnable tryCombine = () -> {
                if (tasksLoaded[0] && gamificationLoaded[0]) {
                    List<CalendarTaskWithTagsAndPomodoro> currentTasks = tasksHolder[0];
                    Gamification currentGamification = gamificationHolder[0];

                    List<CalendarTaskSummary> summaries = (currentTasks != null)
                            ? CalendarExtensions.toTaskSummaries(currentTasks, priorityResolver, dateTimeUtils)
                            : Collections.emptyList();
                    logger.debug(TAG, "execute: Combining data for " + date.toLocalDate() + ". Tasks: " + summaries.size() +
                            ", Gamification loaded: " + (currentGamification != null));
                    result.setValue(new CalendarDashboardData(summaries, currentGamification));
                } else {
                    logger.debug(TAG, "execute: tryCombine - Not all data loaded yet for " + date.toLocalDate() +
                            ". Tasks loaded: " + tasksLoaded[0] + ", Gami loaded: " + gamificationLoaded[0]);
                }
            };

            result.addSource(tasksWithDetailsLiveData, tasks -> {
                logger.debug(TAG, "execute: tasksWithDetailsLiveData emitted for " + date.toLocalDate() + ". Count: " + (tasks != null ? tasks.size() : "null"));
                tasksHolder[0] = tasks;
                tasksLoaded[0] = true;
                tryCombine.run();
            });

            result.addSource(gamificationLiveData, gamification -> {
                logger.debug(TAG, "execute: gamificationLiveData emitted for " + date.toLocalDate() + ". Loaded: " + (gamification != null));
                gamificationHolder[0] = gamification;
                gamificationLoaded[0] = true;
                tryCombine.run();
            });

            return result;
        });
    }
}