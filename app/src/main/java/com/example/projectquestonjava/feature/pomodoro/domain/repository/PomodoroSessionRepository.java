package com.example.projectquestonjava.feature.pomodoro.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

public interface PomodoroSessionRepository {

    LiveData<List<PomodoroSession>> getSessionsForTask(long taskId);

    ListenableFuture<Long> insertSession(PomodoroSession session);

    ListenableFuture<Void> updateSession(PomodoroSession session);

    LiveData<Integer> getCompletedSessionsCount(long taskId); // Считает только FOCUS сессии

    ListenableFuture<PomodoroSession> getLatestSession(long taskId); // Может вернуть null

    LiveData<List<PomodoroSession>> getAllSessionsForUserFlow();

    ListenableFuture<List<PomodoroSession>> getSessionsInPeriod(LocalDateTime startTime, LocalDateTime endTime);

    ListenableFuture<PomodoroSession> getSessionById(long sessionId); // Может вернуть null
}