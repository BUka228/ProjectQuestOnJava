package com.example.projectquestonjava.approach.calendar.domain.usecases;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final Executor ioExecutor;
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

    // Изменяем возвращаемый тип на ListenableFuture
    public ListenableFuture<CalendarDashboardData> execute(LocalDateTime date) {
        logger.debug(TAG, "Invoked for date: " + date);

        // Получаем workspaceId асинхронно
        ListenableFuture<Long> workspaceIdFuture = workspaceSessionManager.getWorkspaceIdFuture();

        return Futures.transformAsync(workspaceIdFuture, workspaceId -> {
            if (workspaceId == null || workspaceId == 0L) {
                logger.warn(TAG, "No active workspace set, returning empty data future.");
                return Futures.immediateFuture(CalendarDashboardData.EMPTY);
            }
            logger.debug(TAG, "Using workspaceId: " + workspaceId);

            // Получаем LiveData, но нам нужно преобразовать его в ListenableFuture для однократного получения
            // Это не самый лучший способ, так как LiveData предназначен для непрерывного наблюдения.
            // В идеале, CalendarRepository.getTasksForDay должен также возвращать ListenableFuture.
            // Для примера, мы возьмем первое значение из LiveData.
            LiveData<List<CalendarTaskWithTagsAndPomodoro>> tasksLiveData = calendarRepository.getTasksForDay(workspaceId, date);
            LiveData<Gamification> gamificationLiveData = gamificationRepository.getCurrentUserGamificationFlow();

            // Создаем ListenableFuture для задач, беря первое не-null значение
            ListenableFuture<List<CalendarTaskWithTagsAndPomodoro>> tasksFuture =
                    LiveDataToFutureConverter.toFuture(tasksLiveData, ioExecutor, Collections.emptyList());

            // Создаем ListenableFuture для геймификации
            ListenableFuture<Gamification> gamificationFuture =
                    LiveDataToFutureConverter.toFuture(gamificationLiveData, ioExecutor, null);


            // Объединяем два ListenableFuture
            return Futures.whenAllSucceed(tasksFuture, gamificationFuture)
                    .call(() -> {
                        List<CalendarTaskWithTagsAndPomodoro> tasks = Futures.getDone(tasksFuture);
                        Gamification gamification = Futures.getDone(gamificationFuture); // Может быть null

                        List<CalendarTaskSummary> summaries = (tasks != null)
                                ? CalendarExtensions.toTaskSummaries(tasks, priorityResolver, dateTimeUtils)
                                : Collections.emptyList();

                        logger.debug(TAG, "Combining data: " + summaries.size() + " tasks, gamification loaded: " + (gamification != null));
                        return new CalendarDashboardData(summaries, gamification);
                    }, ioExecutor); // Выполняем финальное преобразование на ioExecutor

        }, ioExecutor); // Выполняем transformAsync для workspaceIdFuture на ioExecutor
    }

    // Вспомогательный класс для конвертации LiveData в ListenableFuture (однократное получение)
    private static class LiveDataToFutureConverter {
        public static <T> ListenableFuture<T> toFuture(LiveData<T> liveData, Executor executor, @Nullable T defaultValueIfNull) {
            com.google.common.util.concurrent.SettableFuture<T> settableFuture = com.google.common.util.concurrent.SettableFuture.create();
            // Используем MoreExecutors.directExecutor(), чтобы Observer выполнился в том же потоке,
            // что и LiveData.postValue/setValue. Если LiveData обновляется на UI потоке, это нормально.
            // Если LiveData обновляется на фоновом потоке, executor здесь должен быть UI executor.
            // Но так как мы сразу отписываемся, это менее критично.
            Observer<T> observer = new Observer<T>() {
                @Override
                public void onChanged(T t) {
                    liveData.removeObserver(this); // Отписываемся после получения первого значения
                    settableFuture.set(t != null ? t : defaultValueIfNull);
                }
            };
            // Мы должны подписаться на главном потоке, если LiveData обновляется на нем.
            // Для простоты, предположим, что подписка с UI потока безопасна.
            // Если LiveData может эмитить null и это валидное первое значение, то
            // нужно будет дождаться не-null значения или предусмотреть таймаут.
            // В данном случае, если первое значение null, мы его и вернем (или defaultValueIfNull).
            MoreExecutors.directExecutor().execute(() -> liveData.observeForever(observer));

            // На случай, если LiveData уже имеет значение
            T initialValue = liveData.getValue();
            if (initialValue != null) {
                liveData.removeObserver(observer);
                settableFuture.set(initialValue);
            }

            // Добавляем listener, чтобы отписаться, если Future отменили
            settableFuture.addListener(() -> {
                if (settableFuture.isCancelled()) {
                    MoreExecutors.directExecutor().execute(() -> liveData.removeObserver(observer));
                }
            }, executor);

            return settableFuture;
        }
    }
}