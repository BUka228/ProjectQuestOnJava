package com.example.projectquestonjava.feature.statistics.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.projectquestonjava.feature.statistics.data.model.TaskStatistics;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface TaskStatisticsDao {
    // --- ASYNC / LiveData ---
    @Query("UPDATE task_statistics SET was_completed_once = 1 WHERE task_id = :taskId")
    ListenableFuture<Integer> markTaskAsCompletedOnce(long taskId);

    @Query("SELECT ts.* FROM task_statistics ts JOIN Task t ON ts.task_id = t.id WHERE ts.task_id = :taskId AND t.user_id = :userId LIMIT 1")
    ListenableFuture<TaskStatistics> getStatisticsForTaskSuspend(long taskId, int userId);

    @Query("SELECT ts.* FROM task_statistics ts JOIN Task t ON ts.task_id = t.id WHERE ts.task_id IN (:taskIds) AND t.user_id = :userId")
    LiveData<List<TaskStatistics>> getStatisticsForTasks(List<Long> taskIds, int userId);

    @Query("SELECT ts.* FROM task_statistics ts JOIN Task t ON ts.task_id = t.id WHERE ts.task_id = :taskId AND t.user_id = :userId")
    LiveData<TaskStatistics> getTaskStatistics(long taskId, int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insertOrUpdateTaskStatistics(TaskStatistics statistics);

    @Query("UPDATE task_statistics SET completed_pomodoro_focus_sessions = completed_pomodoro_focus_sessions + 1 WHERE task_id = :taskId")
    ListenableFuture<Integer> incrementCompletedPomodoroFocusSessions(long taskId);

    @Query("UPDATE task_statistics SET total_pomodoro_focus_seconds = total_pomodoro_focus_seconds + :secondsToAdd WHERE task_id = :taskId")
    ListenableFuture<Integer> addTotalPomodoroFocusTime(long taskId, int secondsToAdd);

    @Query("UPDATE task_statistics SET total_pomodoro_interruptions = total_pomodoro_interruptions + :countToAdd WHERE task_id = :taskId")
    ListenableFuture<Integer> incrementTotalPomodoroInterruptions(long taskId, int countToAdd);

    @Query("DELETE FROM task_statistics WHERE task_id = :taskId")
    ListenableFuture<Integer> deleteStatisticsForTask(long taskId);

    @Query("UPDATE task_statistics SET time_spent_seconds = time_spent_seconds + :secondsToAdd WHERE task_id = :taskId")
    ListenableFuture<Integer> addTimeToSpent(long taskId, int secondsToAdd);

    @Query("UPDATE task_statistics SET completion_time = :completionTime WHERE task_id = :taskId")
    ListenableFuture<Integer> updateCompletionTime(long taskId, LocalDateTime completionTime);

    @Query("SELECT ts.* FROM task_statistics ts JOIN Task t ON ts.task_id = t.id WHERE t.user_id = :userId")
    LiveData<List<TaskStatistics>> getAllTaskStatistics(int userId);

    @Query("SELECT ts.* FROM task_statistics ts JOIN Task t ON ts.task_id = t.id WHERE t.user_id = :userId AND ts.completion_time BETWEEN :start AND :end")
    ListenableFuture<List<TaskStatistics>> getTaskStatsInPeriod(int userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT ts.* FROM task_statistics ts JOIN Task t ON ts.task_id = t.id WHERE t.user_id = :userId")
    ListenableFuture<List<TaskStatistics>> getAllTaskStatisticsSuspend(int userId);


    // --- SYNC ---
    @Query("SELECT * FROM task_statistics WHERE task_id = :taskId LIMIT 1")
    TaskStatistics getStatisticsForTaskSyncDirect(long taskId); // Уже был

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdateTaskStatisticsSync(TaskStatistics statistics); // Уже был

    @Query("UPDATE task_statistics SET time_spent_seconds = time_spent_seconds + :secondsToAdd WHERE task_id = :taskId")
    void addTimeToSpentSync(long taskId, int secondsToAdd); // Уже был

    @Query("UPDATE task_statistics SET total_pomodoro_focus_seconds = total_pomodoro_focus_seconds + :secondsToAdd WHERE task_id = :taskId")
    void addTotalPomodoroFocusTimeSync(long taskId, int secondsToAdd); // Уже был

    @Query("UPDATE task_statistics SET completed_pomodoro_focus_sessions = completed_pomodoro_focus_sessions + 1 WHERE task_id = :taskId")
    void incrementCompletedPomodoroFocusSessionsSync(long taskId); // Уже был

    @Query("UPDATE task_statistics SET total_pomodoro_interruptions = total_pomodoro_interruptions + :countToAdd WHERE task_id = :taskId")
    void incrementTotalPomodoroInterruptionsSync(long taskId, int countToAdd); // Уже был

    @Query("UPDATE task_statistics SET was_completed_once = 1 WHERE task_id = :taskId")
    void markTaskAsCompletedOnceSync(long taskId); // Уже был

    @Query("UPDATE task_statistics SET completion_time = :completionTime WHERE task_id = :taskId")
    void updateCompletionTimeSync(long taskId, LocalDateTime completionTime); // Уже был
}