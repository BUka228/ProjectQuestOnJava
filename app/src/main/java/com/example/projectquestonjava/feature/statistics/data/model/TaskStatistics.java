package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import com.example.projectquestonjava.core.data.model.core.Task;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Data;



@Entity(
        tableName = "task_statistics",
        primaryKeys = {"task_id"}, // taskId - это primary key
        foreignKeys = {
                @ForeignKey(
                        entity = Task.class,
                        parentColumns = {"id"},
                        childColumns = {"task_id"},
                        onDelete = ForeignKey.CASCADE
                )
        }

)
public class TaskStatistics {

    @ColumnInfo(name = "task_id")
    private final long taskId;

    @Nullable
    @ColumnInfo(name = "completion_time")
    private final LocalDateTime completionTime;

    @ColumnInfo(name = "time_spent_seconds", defaultValue = "0")
    private final int timeSpentSeconds;

    @ColumnInfo(name = "total_pomodoro_focus_seconds", defaultValue = "0")
    private final int totalPomodoroFocusSeconds;

    @ColumnInfo(name = "completed_pomodoro_focus_sessions", defaultValue = "0")
    private final int completedPomodoroFocusSessions;

    @ColumnInfo(name = "total_pomodoro_interruptions", defaultValue = "0")
    private final int totalPomodoroInterruptions;

    @ColumnInfo(name = "was_completed_once", defaultValue = "0")
    private final boolean wasCompletedOnce;

    // Основной конструктор для Room
    public TaskStatistics(long taskId, @Nullable LocalDateTime completionTime, int timeSpentSeconds,
                          int totalPomodoroFocusSeconds, int completedPomodoroFocusSessions,
                          int totalPomodoroInterruptions, boolean wasCompletedOnce) {
        this.taskId = taskId;
        this.completionTime = completionTime;
        this.timeSpentSeconds = timeSpentSeconds;
        this.totalPomodoroFocusSeconds = totalPomodoroFocusSeconds;
        this.completedPomodoroFocusSessions = completedPomodoroFocusSessions;
        this.totalPomodoroInterruptions = totalPomodoroInterruptions;
        this.wasCompletedOnce = wasCompletedOnce;
    }

    // Геттеры
    public long getTaskId() {
        return taskId;
    }

    @Nullable
    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public int getTotalPomodoroFocusSeconds() {
        return totalPomodoroFocusSeconds;
    }

    public int getCompletedPomodoroFocusSessions() {
        return completedPomodoroFocusSessions;
    }

    public int getTotalPomodoroInterruptions() {
        return totalPomodoroInterruptions;
    }

    public boolean isWasCompletedOnce() {
        return wasCompletedOnce;
    }


    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskStatistics that = (TaskStatistics) o;
        return taskId == that.taskId && timeSpentSeconds == that.timeSpentSeconds && totalPomodoroFocusSeconds == that.totalPomodoroFocusSeconds && completedPomodoroFocusSessions == that.completedPomodoroFocusSessions && totalPomodoroInterruptions == that.totalPomodoroInterruptions && wasCompletedOnce == that.wasCompletedOnce && Objects.equals(completionTime, that.completionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, completionTime, timeSpentSeconds, totalPomodoroFocusSeconds, completedPomodoroFocusSessions, totalPomodoroInterruptions, wasCompletedOnce);
    }

    @NonNull
    @Override
    public String toString() {
        return "TaskStatistics{" +
                "taskId=" + taskId +
                ", completionTime=" + completionTime +
                ", timeSpentSeconds=" + timeSpentSeconds +
                ", totalPomodoroFocusSeconds=" + totalPomodoroFocusSeconds +
                ", completedPomodoroFocusSessions=" + completedPomodoroFocusSessions +
                ", totalPomodoroInterruptions=" + totalPomodoroInterruptions +
                ", wasCompletedOnce=" + wasCompletedOnce +
                '}';
    }
}