package com.example.projectquestonjava.feature.pomodoro.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

public interface PomodoroSessionRepository {
    // --- ASYNC / LiveData ---
    LiveData<List<PomodoroSession>> getSessionsForTask(long taskId);
    ListenableFuture<Long> insertSession(PomodoroSession session);
    ListenableFuture<Void> updateSession(PomodoroSession session);
    LiveData<Integer> getCompletedSessionsCount(long taskId);
    ListenableFuture<PomodoroSession> getLatestSession(long taskId);
    LiveData<List<PomodoroSession>> getAllSessionsForUserFlow();
    ListenableFuture<List<PomodoroSession>> getSessionsInPeriod(LocalDateTime startTime, LocalDateTime endTime);
    ListenableFuture<PomodoroSession> getSessionById(long sessionId);

    // --- SYNC ---
    PomodoroSession getSessionByIdSync(long sessionId); // НОВЫЙ (или проверяем)
    void updateSessionSync(PomodoroSession session);     // НОВЫЙ
    long insertSessionSync(PomodoroSession session);    // НОВЫЙ

}