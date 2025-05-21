package com.example.projectquestonjava.feature.pomodoro.data.managers;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroPhase;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSessionRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PomodoroSessionManager {
    private static final String TAG = "PomodoroSessionManager";

    private final PomodoroSessionRepository pomodoroSessionRepository;
    private final Executor ioExecutor;
    private final Logger logger;

    private volatile Long currentActivePhaseSessionId = null;
    private final Object lock = new Object(); // Для синхронизации записи

    @Inject
    public PomodoroSessionManager(
            PomodoroSessionRepository pomodoroSessionRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.pomodoroSessionRepository = pomodoroSessionRepository;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<PomodoroSession> createNewPhaseSession(long taskId, int userId, PomodoroPhase phase) {
        return Futures.submit(() -> {
            logger.debug(TAG, "Creating new DB session for task " + taskId + ", phaseType: " + phase.getType() + ", plannedDuration: " + phase.getDurationSeconds() + "s");
            LocalDateTime startTime = LocalDateTime.now(); // Время начала фазы

            PomodoroSession newSessionEntry = new PomodoroSession(
                    userId, // userId первым, т.к. id автогенерируется
                    taskId,
                    startTime,
                    phase.getType(),
                    phase.getDurationSeconds()
                    // actualDurationSeconds, interruptions, completed будут по умолчанию 0 и false
            );

            try {
                // pomodoroSessionRepository.insertSession возвращает ListenableFuture<Long>
                Long insertedId = pomodoroSessionRepository.insertSession(newSessionEntry).get(); // Блокирующий вызов
                synchronized (lock) {
                    currentActivePhaseSessionId = insertedId;
                }
                logger.info(TAG, "Successfully created new phase session with ID=" + insertedId + " for task " + taskId + ", phase " + phase.getType());
                // Возвращаем объект с присвоенным ID
                newSessionEntry.setId(insertedId); // Устанавливаем ID в объект
                return newSessionEntry;
            } catch (Exception e) {
                synchronized (lock) {
                    currentActivePhaseSessionId = null;
                }
                logger.error(TAG, "Failed to insert new phase session for task " + taskId + ", phase " + phase.getType(), e);
                throw new RuntimeException("Failed to create phase session", e); // Пробрасываем для ListenableFuture
            }
        }, ioExecutor);
    }

    public Long getCurrentPhaseSessionId() {
        return currentActivePhaseSessionId; // Чтение volatile переменной потокобезопасно
    }

    public void resetCurrentPhaseSessionTracking() {
        logger.debug(TAG, "Resetting current phase session tracking. Last known ID: " + currentActivePhaseSessionId);
        synchronized (lock) {
            currentActivePhaseSessionId = null;
        }
    }
}