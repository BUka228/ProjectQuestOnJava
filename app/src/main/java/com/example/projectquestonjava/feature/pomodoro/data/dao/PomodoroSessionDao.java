package com.example.projectquestonjava.feature.pomodoro.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface PomodoroSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insert(PomodoroSession session);

    @Update
    ListenableFuture<Integer> update(PomodoroSession session);

    @Query("SELECT * FROM pomodoro_session WHERE task_id = :taskId AND user_id = :userId ORDER BY start_time DESC")
    LiveData<List<PomodoroSession>> getSessionsForTask(long taskId, int userId);

    @Query("SELECT * FROM pomodoro_session WHERE task_id = :taskId AND user_id = :userId ORDER BY start_time DESC LIMIT 1")
    ListenableFuture<PomodoroSession> getLatestSessionForTask(long taskId, int userId);

    @Query("SELECT COUNT(*) FROM pomodoro_session WHERE task_id = :taskId AND user_id = :userId AND completed = 1 AND session_type = 'FOCUS'")
    LiveData<Integer> getCompletedFocusSessionsCount(long taskId, int userId);

    @Query("DELETE FROM pomodoro_session WHERE task_id = :taskId AND user_id = :userId")
    ListenableFuture<Integer> deleteSessionsForTask(long taskId, int userId);

    @Query("DELETE FROM pomodoro_session WHERE user_id = :userId")
    ListenableFuture<Integer> deleteSessionsForUser(int userId);

    @Query("SELECT * FROM pomodoro_session WHERE user_id = :userId ORDER BY start_time DESC")
    LiveData<List<PomodoroSession>> getAllSessionsForUser(int userId);

    @Query("SELECT * FROM pomodoro_session WHERE user_id = :userId AND start_time BETWEEN :startTime AND :endTime ORDER BY start_time ASC")
    ListenableFuture<List<PomodoroSession>> getSessionsInPeriod(int userId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT * FROM pomodoro_session WHERE id = :sessionId")
    ListenableFuture<PomodoroSession> getSessionById(long sessionId);
}